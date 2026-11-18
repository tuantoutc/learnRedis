package com.example.testredis.service.impl;

import com.example.testredis.domain.record.FlashSaleRequest;
import com.example.testredis.domain.record.FlashSaleResponse;
import com.example.testredis.repository.ProductRepository;
import com.example.testredis.service.FlashSaleService;
import com.example.testredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleServiceImpl implements FlashSaleService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public FlashSaleResponse sale(FlashSaleRequest request) {

        // Trừ kho nguyên tử trực tiếp dưới DB: chỉ trừ khi stock >= quantity
        int updatedRows = productRepository.decreaseStock(request.productId(), request.quantity());

        if (updatedRows == 0) {
            return new FlashSaleResponse(
                    request,
                    "Sản phẩm không tồn tại hoặc không đủ số lượng tồn kho",
                    LocalDateTime.now()
            );
        }
        // Evict cache sau khi transactional commit thành công tránh stale data
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Cache detailCache = cacheManager.getCache("product-detail");
                if(detailCache != null) {
                    detailCache.evict(request.productId());
                }
                Cache listCache = cacheManager.getCache("product-list");
                if(listCache != null) {
                    listCache.clear();
                }
            }
        });
        return new FlashSaleResponse(
                request,
                "Da mua thanh cong "+ request.quantity()+" san pham",
                LocalDateTime.now()
        );
    }

    @Override
    public String resetStockFlashSale(Long productId) {


        return "";
    }
}
