package com.example.testredis.service;


import com.example.testredis.domain.record.CategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getCategories();

    CategoryDto getCategory(Long id);

    CategoryDto deleteCategory(Long id);

    CategoryDto addOrUpdateCategory(CategoryDto categoryDto);
}
