package com.example.testredis.service.impl;

import com.example.testredis.common.anotation.DistributedLock;
import com.example.testredis.domain.entity.ProductEntity;
import com.example.testredis.domain.record.FlashSaleRequest;
import com.example.testredis.domain.record.FlashSaleResponse;
import com.example.testredis.repository.ProductRepository;
import com.example.testredis.service.FlashSaleService;
import com.example.testredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleServiceImpl implements FlashSaleService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CacheManager cacheManager;

    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

//    @Override
//    public FlashSaleResponse sale(FlashSaleRequest request) {
//
//        // Set key lock different lock read cache ProductServiceImpl ("lock:product:" vs "lock:flash-sale")
//        String lockKey = "lock:flash-sale:product" + request.productId();
//        RLock lock = redissonClient.getLock(lockKey);
//        boolean isLocked = false;
//        try{
//            // wait max 5 seconds to get lock
//            // note: Dont send leaseTime ( or sent -1) to active Watchdog auto reset lock if db low processing
//            isLocked = lock.tryLock(5, TimeUnit.SECONDS);
//            if(!isLocked){
//                return new FlashSaleResponse(
//                        request,
//                        "Hệ thống đang bận xử lý đơn hàng khác, vui lòng thử lại sau",
//                        LocalDateTime.now()
//                );
//            }
//            // 2.Open Transaction in LOCK
//            FlashSaleResponse flashSaleResponse = transactionTemplate.execute(status -> {
//                // read latest stock vale  from db
//                ProductEntity productEntity = productRepository.findById(request.productId())
//                        .orElse(null);
//                if(productEntity == null){
//                    return new FlashSaleResponse(request, "Không có sản phẩm này trong dữ liệu", LocalDateTime.now());
//                }
//                if(productEntity.getStock() <= 0) {
//                    return new FlashSaleResponse(request, "Không mua được gì cả do hết hàng", LocalDateTime.now());
//                }
//                // send notify when stock <  request quantity
//                if(request.quantity() > productEntity.getStock()) {
//                    return new FlashSaleResponse(
//                            request,
//                            "Kho chỉ còn" + productEntity.getStock() + " sản phẩm, không đủ số lượng bạn đặt",
//                            LocalDateTime.now());
//                }
//                long newStock = productEntity.getStock() - request.quantity();
//                productEntity.setStock(newStock);
//                productRepository.save(productEntity);
//                return new FlashSaleResponse(
//                        request,
//                        "Da mua thanh cong",
//                        LocalDateTime.now()
//                );
//            });
//
//            //3 Transaction had committed -> process clean cache ( safe because get lock all time)
//            evictProductCache(request.productId());
//            return flashSaleResponse;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("Luồng mua hàng bị ngắt quãng cho productId={}", request.productId(), e);
//            return new FlashSaleResponse(request, "Lỗi hệ thống khi xử lý giao dịch", LocalDateTime.now());
//        }
//        finally {
//            // 4. Chỉ nhả lock sau khi DB đã commit và Cache đã được dọn dẹp
//            if (isLocked && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//
//    }


    @Override
    @DistributedLock(key = "'lock:flash_sale:' + #request.productId()", waitTime = 5, leaseTime = -1)
    @Transactional
    public FlashSaleResponse sale(FlashSaleRequest request) {
        ProductEntity productEntity = productRepository
                .findById(request.productId())
                    .orElse(null);
        if (productEntity == null) {
            return new FlashSaleResponse(request, "Không có sản phẩm này trong dữ liệu", LocalDateTime.now());
        }
        if (productEntity.getStock() <= 0) {
            return new FlashSaleResponse(request, "Không mua được gì cả do hết hàng", LocalDateTime.now());
        }
        if (request.quantity() > productEntity.getStock()) {
            return new FlashSaleResponse(
                    request,
                    "Kho chỉ còn " + productEntity.getStock() + " sản phẩm, không đủ số lượng bạn đặt",
                    LocalDateTime.now()
            );
        }

        productEntity.setStock(productEntity.getStock() - request.quantity());
        productRepository.save(productEntity);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictProductCache(request.productId());
            }
        });

        return new FlashSaleResponse(request, "Đã mua thành công", LocalDateTime.now());
    }

    private void evictProductCache(Long productId) {
        Cache cache = cacheManager.getCache("product-detail");
        if(cache != null){
            cache.evict(productId);
        }
        Cache cacheList = cacheManager.getCache("product-list");
        if(cacheList != null){
            cacheList.clear();
        }
    }

    @Override
    public String resetStockFlashSale(Long productId) {


        return "";
    }
}
