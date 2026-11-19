package com.example.testredis.aspect;

import com.example.testredis.common.anotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1) // Chạy sau ExposeInvocationInterceptor nhưng vẫn bọc ngoài @Transactional (LOWEST_PRECEDENCE)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Around("@annotation(distributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = AopUtils.getMostSpecificMethod(
                signature.getMethod(),
                joinPoint.getTarget().getClass()
        );

        // 1. Phân tích SpEL để tạo Lock Key động và kiểm tra tính hợp lệ
        String lockKey = parseSpelKey(distributedLock.key(), joinPoint.getTarget(), targetMethod, joinPoint.getArgs());

        // 2. Lấy đối tượng RLock từ RedissonClient
        RLock lock = redissonClient.getLock(lockKey);
        boolean isAcquired = false;

        log.debug("[DistributedLock] Đang thử xin Lock cho key: {} (Wait: {}, Lease: {}, Unit: {})",
                lockKey, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

        try {
            // 3. Thử lấy khóa trong khoảng thời gian waitTime
            isAcquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!isAcquired) {
                log.warn("[DistributedLock] Không thể lấy Lock cho key: {}. Hệ thống đang quá tải!", lockKey);
                throw new IllegalStateException("Hệ thống bận, vui lòng thử lại sau!");
            }

            log.debug("[DistributedLock] Lấy Lock thành công cho key: {}", lockKey);

            // 4. Thực thi business logic (bên trong có thể có @Transactional)
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[DistributedLock] Luồng bị gián đoạn khi đang chờ Lock cho key: {}", lockKey, e);
            throw new IllegalStateException("Yêu cầu bị gián đoạn khi chờ xử lý, vui lòng thử lại!", e);
        } finally {
            // 5. Giải phóng khóa an toàn
            if (isAcquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.debug("[DistributedLock] Đã giải phóng Lock cho key: {}", lockKey);
                } catch (IllegalMonitorStateException e) {
                    log.warn("[DistributedLock] Lock cho key {} đã hết hạn hoặc bị giải phóng trước đó", lockKey, e);
                }
            }
        }
    }

    /**
     * Phân tích biểu thức SpEL với cache Expression và fallback sang tên method nếu key trống.
     */
    private String parseSpelKey(String spelKey, Object target, Method method, Object[] args) {
        if (!StringUtils.hasText(spelKey)) {
            return method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }

        EvaluationContext context = new MethodBasedEvaluationContext(target, method, args, nameDiscoverer);
        Expression expression = expressionCache.computeIfAbsent(spelKey, parser::parseExpression);
        String lockKey = expression.getValue(context, String.class);

        if (!StringUtils.hasText(lockKey)) {
            throw new IllegalArgumentException(
                    "Biểu thức SpEL '" + spelKey + "' trên method '" + method.getName() + "' trả về key rỗng hoặc null"
            );
        }

        return lockKey;
    }
}