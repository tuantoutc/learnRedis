package com.example.testredis.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageHeader(
        int pageNo,
        int pageSize,
        String sortBy,
        String sortType
) {
        public PageHeader {
                if (pageNo < 0) pageNo = 0;
                if (pageSize <= 0) pageSize = 10;
                if (sortBy == null || sortBy.isBlank()) sortBy = "id";
                if (sortType == null || sortType.isBlank()) sortType = "asc";
        }
         public Pageable toPageable() {
                 Sort sort = sortType.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
              return PageRequest.of((pageNo == 0 ? 0:pageNo -1 ) ,pageSize, sort);
         }
}
