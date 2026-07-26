package ru.practicum.mapper;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CategoryMapper {

    CategoryDto mapToCategoryDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category mapToCategory(NewCategoryDto newCategoryDto);
}