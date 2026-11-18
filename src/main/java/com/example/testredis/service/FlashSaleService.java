package com.example.testredis.service;

import com.example.testredis.domain.record.FlashSaleRequest;
import com.example.testredis.domain.record.FlashSaleResponse;

public interface FlashSaleService {

    FlashSaleResponse sale(FlashSaleRequest request);

    String resetStockFlashSale(Long productId);

}
