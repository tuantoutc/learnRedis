package com.example.testredis.service;

import com.example.testredis.domain.record.CustomPageResponse;
import com.example.testredis.domain.record.ProductRequest;
import com.example.testredis.domain.record.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    CustomPageResponse<ProductResponse> searchProducts(Pageable pageable);

    ProductResponse getProduct(Long id);

    Void deleteProduct(Long productId);

    ProductResponse addProduct(ProductRequest productRequest);

    ProductResponse updateProduct(Long id, ProductRequest productRequest);


}
