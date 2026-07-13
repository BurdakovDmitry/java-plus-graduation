package ewm.category.mapper;

import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;
import ewm.category.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category toEntity(NewCategoryDto newCategoryDto);
}