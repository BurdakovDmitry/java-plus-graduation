package ru.practicum.service;

import ru.practicum.contract.event.EventClient;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventClient eventClient;

    /**
     * Создает и сохраняет новую категорию в системе.
     *
     * @param categoryDto с данными для регистрации новой категории
     * @throws ConflictException если категория с указанным именем уже зарегистрирована в базе данных
     * @return CategoryDto созданной категории с присвоенным ID
     */
    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto categoryDto) {
        log.info("Добавление новой категории: {}", categoryDto.name());

        Category category = categoryMapper.mapToCategory(categoryDto);

        if (categoryRepository.existsByName(categoryDto.name())) {
            log.warn("Уже существует категория с именем: {}", categoryDto.name());
            throw new ConflictException("Категория с именем= " + categoryDto.name() + " уже существует");
        }

        Category saveCategory = categoryRepository.save(category);
        log.info("Добавлена категория с ID={}", saveCategory.getId());

        return categoryMapper.mapToCategoryDto(category);
    }

    /**
     * Удаляет категорию из системы по ее идентификатору.
     * Перед удалением проверяет существование категории.
     *
     * @param categoryId идентификатор категории
     * @throws ConflictException если категория с указанным ID не зарегистрирована в базе данных или есть связанные события
     */
    @Override
    @Transactional
    public void deleteCategoryById(Long categoryId) {
        log.info("Удаление категории с ID={}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new ConflictException("Категория с ID=" + categoryId + " не найдена");
        }

        if (eventClient.isCategory(categoryId)) {
            throw new ConflictException("Существуют события, связанные с категорией с ID=" + categoryId);
        }

        categoryRepository.deleteById(categoryId);
        log.info("Категория с ID={} удалена", categoryId);
    }

    /**
     * Обновляет имя категории по ее идентификатору.
     * Имя категории должно быть уникальным.
     *
     * @param categoryId идентификатор категории
     * @param categoryDto с данными для регистрации нового имени категории
     * @throws ConflictException если категория с указанным именем уже зарегистрирована в базе данных
     * @return CategoryDto обновленной категории с новым именем
     */
    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto categoryDto) {
        log.info("Обновление категории с ID={}, новое имя={}", categoryId, categoryDto.name());

        Category category = existsCategory(categoryId);

        if (categoryRepository.existsByNameAndIdNot(categoryDto.name(), categoryId)) {
            log.warn("Ошибка обновления. Категория с именем={} уже существует", categoryDto.name());
            throw new ConflictException("Категория с именем= " + categoryDto.name() + " уже существует");
        }

        category.setName(categoryDto.name());
        Category updateCategory = categoryRepository.save(category);

        log.info("Категория с ID={} обновлена. Новое имя={}", categoryId, updateCategory.getName());
        return categoryMapper.mapToCategoryDto(updateCategory);
    }

    /**
     * Возвращает список категорий по заданным фильтрам с поддержкой пагинации и с сортировкой по ID.
     *
     * @param from указывает смещение
     * @param size указывает размер страницы
     * @return список CategoryDto отфильтрованных категорий (может быть пустым)
     */
    @Override
    public List<CategoryDto> getAllCategory(Integer from, Integer size) {
        log.info("Поиск категорий по фильтрам: from={}, size={}", from, size);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<CategoryDto> categories = categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::mapToCategoryDto)
                .toList();

        log.info("Поиск по фильтрам завершен. Найдено категорий: {}", categories.size());
        return categories;
    }

    /**
     * Возвращает список категорий согласно списку ID.
     *
     * @param ids со списком запрашиваемых ID
     * @return список CategoryDto категорий согласно списку ID (может быть пустым)
     */
    @Override
    public List<CategoryDto> findAllByIdIn(List<Long> ids) {
        log.info("Поиск категорий по списку ID: ids={}", ids);

        List<CategoryDto> categories = categoryRepository.findAllByIdIn(ids).stream()
                .map(categoryMapper::mapToCategoryDto)
                .toList();

        log.info("Поиск по списку ID завершен. Найдено категорий: {}", categories.size());
        return categories;
    }

    /**
     * Возвращает DTO категории из системы по ее идентификатору.
     *
     * @param categoryId идентификатор категории
     * @throws NotFoundException если категория с указанным ID не зарегистрирована в базе данных
     */
    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        log.info("Получение категории с ID={}", categoryId);

        Category category = existsCategory(categoryId);

        log.info("Получена категория с ID={}", categoryId);
        return categoryMapper.mapToCategoryDto(category);
    }

    /**
     * Возвращает категорию из системы по его идентификатору.
     *
     * @param categoryId идентификатор категории
     * @throws NotFoundException если категория с указанным ID не зарегистрирована в базе данных
     */
    private Category existsCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с ID=" + categoryId + " не найдена"));
    }
}
