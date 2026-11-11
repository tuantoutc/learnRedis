package com.example.testredis.domain.record;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

public record CustomPageResponse<T>(
        List<T> content,
        int pageNo,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last

) implements Serializable {

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
    public static <T> CustomPageResponse<T> from(Page<T> page) {
        return new CustomPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
