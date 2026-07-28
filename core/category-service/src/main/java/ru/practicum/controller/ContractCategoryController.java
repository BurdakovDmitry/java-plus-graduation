package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.service.CategoryService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping(path = "/contract/category")
@RequiredArgsConstructor
public class ContractCategoryController {
    private final CategoryService categoryService;

    @GetMapping("/categoriesIds")
    public List<CategoryDto> getCategoriesByIds(@RequestParam List<Long> ids) {
        log.info("Внутренний запрос: получение категорий по списку ID: {}", ids);
        return categoryService.findAllByIdIn(ids);
    }
}
