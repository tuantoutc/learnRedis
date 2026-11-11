package com.example.testredis.service.impl;

import com.example.testredis.domain.entity.CategoryEntity;
import com.example.testredis.domain.record.CategoryDto;
import com.example.testredis.repository.CategoryRepository;
import com.example.testredis.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> getCategories() {
        List<CategoryEntity> list = categoryRepository.findAll();
        List<CategoryDto> categoryDtos = list.stream().filter(Objects::nonNull)
                .map(item -> new CategoryDto(
                        item.getId(),
                        item.getName(),
                        item.getDescription()))
                .toList();
        return List.of();
    }

    @Override
    public CategoryDto getCategory(Long id) {
        Optional<CategoryEntity> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            return null;
        }
        CategoryEntity categoryEntity = category.get();
        return new CategoryDto(
                categoryEntity.getId(),
                categoryEntity.getName(),
                categoryEntity.getDescription());
    }

    @Override
    public CategoryDto deleteCategory(Long id) {
        return null;
    }

    @Override
    @Transactional
    public CategoryDto addOrUpdateCategory(CategoryDto request) {
        CategoryEntity category;
        if (Objects.isNull(request.id())) {
            category = CategoryEntity.builder()
                    .name(request.name())
                    .description(request.description())
                    .build();
        } else {
            Optional<CategoryEntity> currentCategory = categoryRepository.findById(request.id());
            if (currentCategory.isEmpty()) {
                throw new RuntimeException("Category not found");
            }
            category = currentCategory.get();
            category.setDescription(request.description());
            category.setName(request.name());
        }
        CategoryEntity saved = categoryRepository.save(category);

        return new CategoryDto(saved.getId(), saved.getName(), saved.getDescription());
    }
}
