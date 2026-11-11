package com.example.testredis.controller;


import com.example.testredis.common.ApiResponse;
import com.example.testredis.domain.record.CustomPageResponse;
import com.example.testredis.domain.record.ProductRequest;
import com.example.testredis.domain.record.ProductResponse;
import com.example.testredis.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
//        Sort sort = page.sortType().equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(page.sortBy()).ascending() : Sort.by(page.sortBy()).descending();
//        Pageable pageable = PageRequest.of((page.pageNo() == 0 ? 0:page.pageNo() -1 ) , page.pageSize(), sort);
        return ApiResponse.success(productService.getProduct(id));
    }


    @PutMapping("")
    public ApiResponse<ProductResponse> updateProduct(@RequestParam(value = "id") Long id,
                                                      @Valid @RequestBody ProductRequest productRequest) {

        return ApiResponse.success(productService.updateProduct(id, productRequest));
    }

    @GetMapping("")
    public ApiResponse<CustomPageResponse<ProductResponse>> searchProduct( @RequestParam(value = "page_no", defaultValue = "1") int pageNo,
                                                                           @RequestParam(value = "page_size", defaultValue ="10") int pageSize,
                                                                           @RequestParam(defaultValue = "id") String sortBy,
                                                                           @RequestParam(defaultValue = "asc") String sortType) {

        Sort sort = sortType.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        int pageIndex = Math.max(0, pageNo - 1);
        Pageable pageable = PageRequest.of(pageIndex , pageSize, sort);
        return ApiResponse.success(productService.searchProducts(pageable));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success("Product deleted");
    }

}
