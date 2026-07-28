package ru.practicum.contract.category;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

@FeignClient(name = "category-service", fallbackFactory = CategoryClientFallbackFactory.class)
public interface CategoryClient {
    @GetMapping("/categories/{catId}")
    CategoryDto getCategoryById(@PathVariable("catId") Long catId);

    @GetMapping("/contract/category/categoriesIds")
    List<CategoryDto> getCategoriesByIds(@RequestParam("ids") List<Long> ids);
}
