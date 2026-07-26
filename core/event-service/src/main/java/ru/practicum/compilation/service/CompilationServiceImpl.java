package ru.practicum.compilation.service;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.contract.request.ParticipationRequestClient;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.dto.request.ConfirmedRequestCount;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final ParticipationRequestClient requestClient;
    private final EventService eventService;

    /**
     * Создает и сохраняет новою подборку событий в системе
     * Если список событий не передан, то создает подборку без событий
     *
     * @param newCompilationDto с данными для регистрации новой подборки
     * @return созданной подборки с присвоенным ID
     */
    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        log.info("Добавление новой подборки: {}", newCompilationDto.title());

        Compilation compilation = compilationMapper.postDtoToCompilation(newCompilationDto);

        if (newCompilationDto.events() != null && !newCompilationDto.events().isEmpty()) {
            List<Event> events = eventRepository.findAllById(newCompilationDto.events());
            compilation.setEvents(events);
        } else {
            compilation.setEvents(new ArrayList<>());
        }

        Compilation savedCompilation = compilationRepository.save(compilation);

        CompilationDto dto = compilationMapper.compilationToDto(savedCompilation);
        saveViewsAndConfirmedRequests(List.of(dto), savedCompilation.getEvents());

        log.info("Добавлена новая подборка: {}", newCompilationDto.title());
        return dto;
    }

    /**
     * Удаляет подборку по его идентификатору.
     * Перед удалением проверяем, что такая подборка существует в системе
     *
     * @param compilationId идентификатор подборки
     */
    @Override
    @Transactional
    public void delete(Long compilationId) {
        log.info("Удаление подборки с ID={}", compilationId);

        Compilation compilation = findCompilationById(compilationId);

        log.info("Подборка с ID={} успешна удалена", compilationId);
        compilationRepository.delete(compilation);
    }

    /**
     * Обновляет подборку событий по его идентификатору.
     *
     * @param compilationId идентификатор подборки
     * @param updCompilationDto с данными для обновления подборки событий
     * @return CompilationDto обновленную подборку с актуальным данными
     */
    @Override
    @Transactional
    public CompilationDto update(UpdateCompilationDto updCompilationDto, Long compilationId) {
        log.info("Обновление подборки с ID={}", compilationId);

        Compilation compilation = findCompilationById(compilationId);

        if (updCompilationDto.events() != null && !updCompilationDto.events().isEmpty()) {
            List<Event> events = eventRepository.findAllById(updCompilationDto.events());
            compilation.setEvents(events);
        }

        compilationMapper.updateDtoToCompilation(compilation, updCompilationDto);
        Compilation savedCompilation = compilationRepository.save(compilation);

        CompilationDto dto = compilationMapper.compilationToDto(savedCompilation);
        saveViewsAndConfirmedRequests(List.of(dto), savedCompilation.getEvents());

        log.info("Подборка с ID={} успешна обновлена", compilationId);
        return dto;
    }

    /**
     * Возвращает список подборок событий с поддержкой фильтрации по закреплению и пагинации.
     *
     * @param pinned флаг закрепления подборки на главной странице
     * @param from   количество элементов, которые нужно пропустить для начала отсчета
     * @param size   количество элементов в наборе (размер страницы)
     * @return список CompilationDto отфильтрованных подборок (может быть пустым)
     */
    @Override
    public List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size) {
        log.info("Поиск подборок событий по фильтрам: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, pageable);

        if (compilations.isEmpty()) {
            return List.of();
        }

        List<Event> events = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .distinct()
                .toList();

        List<CompilationDto> dto = compilations.stream()
                .map(compilationMapper::compilationToDto)
                .collect(Collectors.toList());

        saveViewsAndConfirmedRequests(dto, events);

        log.info("Поиск завершен. Найдено подборок: {}", dto.size());
        return dto;
    }

    /**
     * Возвращает подборку событий по её идентификатору.
     *
     * @param compId идентификатор подборки
     * @return CompilationDto найденной подборки событий
     */
    @Override
    public CompilationDto getCompilation(Long compId) {
        log.info("Получение подборки с ID={}", compId);

        Compilation compilation = findCompilationById(compId);
        CompilationDto dto = compilationMapper.compilationToDto(compilation);

        saveViewsAndConfirmedRequests(List.of(dto), compilation.getEvents());

        log.info("Подборка событий с ID={} успешно получена", compId);
        return dto;
    }

    /**
     * Возвращает подборку событий по её идентификатору.
     *
     * @param compId идентификатор подборки
     * @throws NotFoundException если подборка с указанным ID не зарегистрирована в базе данных
     * @return Compilation найденной подборки событий
     */
    private Compilation findCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Подборка с ID=%d не найдена", compId)));
    }

    /**
     * Пакетно заполняет DTO подборок информацией о просмотрах и подтверждённых запросах для EventShortDto.
     *
     * @param dto список CompilationDto подборок событий
     * @param events список сущностей Event, содержащихся в подборках
     */
    private void saveViewsAndConfirmedRequests(List<CompilationDto> dto, List<Event> events) {
        if (dto.isEmpty() || events.isEmpty()) {
            return;
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        // Пакетный запрос в request-service для получения подтверждённых заявок по всем событиям
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestCount(eventIds).stream()
                .collect(Collectors.toMap(ConfirmedRequestCount::eventId, ConfirmedRequestCount::count));

        // Получение статистики просмотров для всей пачки событий из микросервиса статистики
        Map<Long, Long> viewsMap = eventService.getViewsMap(events, false);

        // Распределение собранных данных в список EventShortDto
        dto.forEach(comDto -> {
            if (comDto.events() != null) {
                comDto.events().forEach(shortDto -> {
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
                });
            }
        });
    }
}
