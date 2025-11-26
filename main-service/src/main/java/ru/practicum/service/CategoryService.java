package ru.practicum.service;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto newCategoryDto);
    void deleteCategory(Long catId);
    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);
    List<CategoryDto> getCategories(int from, int size);
    CategoryDto getCategory(Long catId);
}