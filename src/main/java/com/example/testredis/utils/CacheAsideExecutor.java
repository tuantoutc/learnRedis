package com.example.testredis.utils;


import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CacheAsideExecutor {

    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;
    // Giá trị đại diện cho Null lưu thẳng vào Redis (nhìn thấy rõ trên RedisInsight)

    public <K, V> V execute(String cacheName, K key, Class<V> type, Supplier<V> dbLoader) {
        Cache cache = cacheManager.getCache(cacheName);
        Cache.ValueWrapper hit = getFromCache(cache, key);
        if (hit != null) {
            return type.cast(hit.get());
        }

        RLock lock = redissonClient.getLock("lock:" + cacheName + ":" + key);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            // Trường hợp 1: Hết 3s chờ mà vẫn KHÔNG lấy được lock
            if (!locked) {
                // Thử đọc cache lần cuối đề phòng luồng giữ lock vừa nhả ra đúng khoảnh khắc hết 3s
                Cache.ValueWrapper lastChance = getFromCache(cache, key);
                if (lastChance != null) {
                    return type.cast(lastChance.get());
                }
                throw new IllegalStateException("He thong dang qua tai, vui long thu lai sau it phut");
            }
            // Trường hợp 2: Đã chiếm được lock thành công -> Double-check trước khi query DB
            Cache.ValueWrapper recheck = getFromCache(cache, key);
            if (recheck != null) {
                return type.cast(recheck.get());
            }

            V data = dbLoader.get();
            if (cache != null) {
                cache.put(key, data);
            }
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Luong xu ly bi gian doan", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    private Cache.ValueWrapper getFromCache(Cache cache, Object key) {
        return cache != null ? cache.get(key) : null;
    }
}
