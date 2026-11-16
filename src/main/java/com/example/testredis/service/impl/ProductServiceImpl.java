package com.example.testredis.service.impl;

import com.example.testredis.domain.entity.ProductEntity;
import com.example.testredis.domain.record.CustomPageResponse;
import com.example.testredis.domain.record.ProductRequest;
import com.example.testredis.domain.record.ProductResponse;
import com.example.testredis.repository.ProductRepository;
import com.example.testredis.service.ProductService;
import com.example.testredis.utils.CacheAsideExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {


    @Lazy
    @Autowired
    private ProductService self;

    private static final String DETAIL_CACHE = "product-detail";

    private final ProductRepository productRepository;
    private final RBloomFilter<Long> productBloomFilter;
    private final CacheManager cacheManager;

    private final CacheAsideExecutor executor;


    @Override
    @Cacheable(
            value = "product-list",
            key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort"
    )
    public CustomPageResponse<ProductResponse> searchProducts(Pageable pageable) {
        Page<ProductEntity> page = productRepository.findAll(pageable);
        Page<ProductResponse> pageProductResponse = page.map(item -> new ProductResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategoryId(),
                item.getStock()
        ));
        return CustomPageResponse.from(pageProductResponse);
    }

    @Override
    public ProductResponse getProduct(Long id) {
//        Cache cache = cacheManager.getCache(DETAIL_CACHE);
//
////         1. Đọc Cache lần 1 (wrapper != null nghĩa là đã có key, kể cả khi giá trị bên trong là null)
//        Cache.ValueWrapper hit = getFromCache(cache, id);
//        if (hit != null) {
//            return (ProductResponse) hit.get();
//        }
//
//        // 2. Chặn nhanh qua Bloom Filter
//        if (!productBloomFilter.contains(id)) {
//            return null;
//        }
//
//        // 3. Distributed Lock chống nhiều request cùng xuyên qua DB khi cache miss
//        RLock lock = redissonClient.getLock("lock:product:" + id);
//        boolean locked = false;
//        try {
//            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
//            if (!locked) {
//                // Hết 3s chờ khóa: thử đọc lại cache lần cuối, tuyệt đối KHÔNG query DB                Cache cache = cacheManager.getCache(DETAIL_CACHE);
//                Cache.ValueWrapper retryHit = getFromCache(cache, id);
//                if (retryHit != null) {
//                    return (ProductResponse) retryHit.get();
//                }
//                throw new IllegalStateException("Hệ thống đang quá tải, vui lòng thử lại sau.");
//            }
//            // 4. Double-check cache sau khi chiếm được khóa
//            Cache.ValueWrapper doubleCheckHit = getFromCache(cache, id);
//            if (doubleCheckHit != null) {
//                return (ProductResponse) doubleCheckHit.get();
//            }
//            // 5. Thực sự miss -> Query DB
//            ProductResponse response = productRepository.findById(id)
//                    .map(this::mapToResponse)
//                    .orElse(null);
//
//            // 6. Ghi vào Redis TRƯỚC KHI nhả khóa ở khối finally
//            // Nếu response == null, Spring RedisCache tự bọc thành NullValue (TTL 30s)
//            if (cache != null) {
//                cache.put(id, response);
//            }
//            return response;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new IllegalStateException("Luồng xử lý bị gián đoạn", e);
//        } finally {
//            if (locked && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }

        // chặn nhanh cac id ko ton tai qua bloom filter
        if (!productBloomFilter.contains(id)) {
            return null;
        }

        // goi CacheAsideExecutor xu ly tron goi cache + distributed lock + db fallback
        return executor.execute(
                DETAIL_CACHE,
                id,
                ProductResponse.class,
                () -> productRepository
                        .findById(id)
                        .map(this::mapToResponse)
                        .orElse(null)
        );
    }

    private Cache.ValueWrapper getFromCache(Cache cache, Long id) {
        return cache != null ? cache.get(id) : null;
    }


    private ProductResponse mapToResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategoryId(),
                entity.getStock()
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product-detail", key = "#productId"),
            @CacheEvict(value = "product-list", allEntries = true)
    })
    public Void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(productId);
        return null;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product-list", allEntries = true)
    public ProductResponse addProduct(ProductRequest productRequest) {
        ProductEntity entity = ProductEntity.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .categoryId(productRequest.categoryId())
                .stock(productRequest.stock())
                .build();
        ProductEntity saved = productRepository.save(entity);
        Long savedId = saved.getId();

        // Chỉ cập nhật BloomFilter và xóa NullValue cache SAU KHI DB đã commit thành công
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productBloomFilter.add(savedId);
                Cache detailCache = cacheManager.getCache(DETAIL_CACHE);
                if (detailCache != null) {
                    detailCache.evict(savedId); // Xóa đúng key "product-detail::<savedId>"
                }
                log.info("After commit: Đã nạp ID {} vào BloomFilter và evict null cache", savedId);
            }
        });

        return mapToResponse(saved);
    }


    @Override
    @Transactional
    @Caching(
            put = {@CachePut(value = "product-detail", key = "#id")},
            evict = {@CacheEvict(value = "product-list", allEntries = true)}
    )
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productEntity.setName(productRequest.name());
        productEntity.setDescription(productRequest.description());
        productEntity.setPrice(productRequest.price());
        productEntity.setCategoryId(productRequest.categoryId());
        productEntity.setStock(productRequest.stock());
        productRepository.save(productEntity);
        return new ProductResponse(
                productEntity.getId(),
                productEntity.getName(),
                productEntity.getDescription(),
                productEntity.getPrice(),
                productEntity.getCategoryId(),
                productEntity.getStock());
    }
}
