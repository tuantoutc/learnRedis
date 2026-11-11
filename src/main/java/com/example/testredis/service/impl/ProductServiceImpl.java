package com.example.testredis.service.impl;

import com.example.testredis.domain.entity.ProductEntity;
import com.example.testredis.domain.record.CustomPageResponse;
import com.example.testredis.domain.record.ProductRequest;
import com.example.testredis.domain.record.ProductResponse;
import com.example.testredis.repository.ProductRepository;
import com.example.testredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {


    @Lazy
    @Autowired
    private ProductService self;

    private final ProductRepository productRepository;

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
    @Cacheable(value = "product-detail", key = "#id")
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(this::maptoResponse)
                .orElse(null);

    }

    private ProductResponse maptoResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategoryId(),
                entity.getStock());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product-detail", key = "#id"),
            @CacheEvict(value = "product-list", allEntries = true)
    })
    public Void deleteProduct(Long productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
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
        productRepository.save(entity);
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategoryId(),
                entity.getStock());
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
