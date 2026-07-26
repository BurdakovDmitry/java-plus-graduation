package ru.practicum.contract.category;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class CategoryClientFallbackFactory implements FallbackFactory<CategoryClient> {
    @Override
    public CategoryClient create(Throwable cause) {
        log.error("Сбой при вызове сервиса category-service. Причина: {}", cause.getMessage());

        return new CategoryClient() {
            @Override
            public CategoryDto getCategoryById(Long catId) {
                log.error("Fallback для getCategoryById: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления категориями временно недоступен.");
            }

            @Override
            public List<CategoryDto> getCategoriesByIds(List<Long> ids) {
                log.error("Fallback для getCategoriesByIds: сервис временно недоступен");
                return List.of();
            }
        };
    }
}
