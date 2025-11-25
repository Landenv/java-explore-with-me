package ru.practicum.service;

import ru.practicum.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto);
    void deleteCategory(Long catId);
    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);
    List<CategoryDto> getCategories(int from, int size);
    CategoryDto getCategory(Long catId);
}