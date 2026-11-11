package com.example.testredis.domain.record;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Double price,
        Long categoryId,
        Long stock
) {
}
