package com.example.testredis.domain.record;

import jakarta.validation.Valid;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FlashSaleResponse(
        @Valid FlashSaleRequest request,
        String response,
        LocalDateTime responseDate

) implements Serializable {
}
