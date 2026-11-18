package com.example.testredis.domain.record;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

public record FlashSaleRequest(
        @NotNull Long productId,
        @NotNull Long userId,
        @Positive(message = "So luong phai lon hon 0")
        @NotNull(message = "Khong duoc de trong gia tri nay")
        Long quantity
) implements Serializable {
}
