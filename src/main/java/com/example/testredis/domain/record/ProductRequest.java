package com.example.testredis.domain.record;

import jakarta.validation.constraints.*;

public record ProductRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
        String name,
        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description,
        @NotNull(message = "Giá sản phẩm không được để trống")
        @Positive(message = "Giá sản phẩm phải lớn hơn 0")
        Double price,
        @NotNull(message = "ID danh mục không được để trống")
        Long categoryId,
        @NotNull(message = "Số lượng tồn kho không được để trống")
        @PositiveOrZero(message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
        Long stock
) {
}
