package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService adminCategoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategories(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("POST /admin/categories: newCategoryDto={}", newCategoryDto);
        return adminCategoryService.addCategory(newCategoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategories(@PathVariable("catId") @Positive Long categoryId) {
        log.info("DELETE /admin/categories/{}", categoryId);
        adminCategoryService.deleteCategoryById(categoryId);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(@PathVariable("catId") @Positive Long categoryId,
                                      @Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("PATCH /admin/categories/{}: newCategoryDto={}", categoryId, newCategoryDto);
        return adminCategoryService.updateCategory(categoryId, newCategoryDto);
    }
}
