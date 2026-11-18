package com.example.testredis.controller;

import com.example.testredis.common.ApiResponse;
import com.example.testredis.domain.record.FlashSaleRequest;
import com.example.testredis.domain.record.FlashSaleResponse;
import com.example.testredis.service.FlashSaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flash-sale")
public class FlashSaleController {


    private final FlashSaleService flashSaleService;

    @PostMapping("/sale-product")
    public ApiResponse<FlashSaleResponse> flashSale(
            @RequestBody @Valid FlashSaleRequest request
    ) {
        return ApiResponse.success(flashSaleService.sale(request));
    }


    @PutMapping("/reset-stock")
    public ApiResponse<String> resetStock( @NotNull @RequestParam(value = "productId")  Long productId) {
        return ApiResponse.success(flashSaleService.resetStockFlashSale(productId));
    }


}
