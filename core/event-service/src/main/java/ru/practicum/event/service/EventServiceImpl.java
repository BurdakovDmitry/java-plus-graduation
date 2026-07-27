package ru.practicum.event.service;

import client.StatClient;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import ewm.HitDto;
import ewm.ParamDto;
import ewm.StatsDto;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.contract.request.ParticipationRequestClient;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.dto.AdminEventSearchFilter;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PublicEventParamDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.event.model.Event;
import ru.practicum.dto.event.EventState;
import ru.practicum.contract.category.CategoryClient;
import ru.practicum.contract.user.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.event.model.QEvent;
import ru.practicum.exception.ValidationException;
import ru.practicum.dto.request.ParticipationStatus;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final ParticipationRequestClient requestClient;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final EventMapper eventMapper;
    private final CategoryClient categoryClient;
    private final StatClient statClient;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Приватное получение упрощенного списка событий по заданным фильтрам с поддержкой пагинации.
     * Использует QueryDSL для построения динамических критериев поиска.
     * Если сервисы не доступны, то возвращаются заглушки
     *
     * @param userId идентификатор текущего пользователя
     * @param from   количество элементов, которые нужно пропустить для начала отсчета
     * @param size   количество элементов в наборе (размер страницы)
     * @return список EventShortDto упрощенных и отфильтрованных событий
     */
    @Override
    public List<EventShortDto> getEventsPrivate(Long userId, Integer from, Integer size) {
        log.info("Поиск событий пользователем с ID={} по фильтрам: from={}, size={}", userId, from, size);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> requestsMap = confirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsMap(events, false);
        Map<Long, CategoryDto> categoryDtoMap = categoriesMap(events);
        UserShortDto userDto = getUserWithFallback(userId);

        List<EventShortDto> eventsShortDto = events.stream()
                .map(event -> {
                    CategoryDto categoryDto = categoryDtoMap.getOrDefault(event.getCategoryId(),
                            new CategoryDto(event.getCategoryId(), "Unknown Category"));
                    EventShortDto shortDto = eventMapper.toShortDto(event, userDto, categoryDto);
                    shortDto.setConfirmedRequests(requestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
                    return shortDto;
                })
                .toList();

        log.info("Поиск завершен. Найдено событий: {}", eventsShortDto.size());
        return eventsShortDto;
    }

    /**
     * Создает и сохраняет новое событие в системе.
     *
     * @param userId идентификатор создателя события
     * @param dto с данными для регистрации нового события
     * @return EventFullDto созданного события с присвоенным ID
     */
    @Override
    @Transactional
    public EventFullDto addEventPrivate(Long userId, NewEventDto dto) {
        log.info("Добавление нового события пользователем с ID: {}", userId);

        if (dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата и время, не могут быть раньше, чем через два часа от текущего момента");
        }

        UserShortDto userDto = userClient.findById(userId);
        CategoryDto categoryDto = categoryClient.getCategoryById(dto.category());

        // Используем маппер для создания события
        Event event = eventMapper.toEvent(dto);

        event.setCategoryId(categoryDto.id());
        event.setInitiatorId(userDto.id());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event saved = eventRepository.save(event);

        log.info("Добавлено новое событие с ID={}", saved.getId());
        return eventMapper.toFullDto(saved, userDto, categoryDto);
    }

    /**
     * Приватное получение события по заданному идентификатору.
     * Если сервисы не доступны, то возвращаются заглушки
     *
     * @param userId идентификатор текущего пользователя
     * @param eventId идентификатор события
     * @param url объект HTTP-запроса для фиксации просмотра в статистике
     * @throws ConflictException если текущий пользователь не является автором события
     * @return EventFullDto события с полными данными
     */
    @Override
    public EventFullDto getEventByIdPrivate(Long userId, Long eventId, String url) {
        log.info("Поиск события с ID={} пользователем с ID={}", eventId, userId);

        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Событие может запросить только инициатор");
        }

        UserShortDto userDto = getUserWithFallback(userId);
        CategoryDto categoryDto = getCategoryWithFallback(event.getCategoryId());

        LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn();
        ParamDto paramDto = new ParamDto(start, LocalDateTime.now(), List.of(url), false);

        EventFullDto fullDto = eventMapper.toFullDto(event, userDto, categoryDto);

        fullDto.setConfirmedRequests(getConfirmedRequestsWithFallback(eventId, ParticipationStatus.CONFIRMED));
        fullDto.setViews(getViews(paramDto));

        log.info("Поиск события с ID={} пользователем с ID={} успешно завершен", eventId, userId);
        return fullDto;
    }

    /**
     * Обновляет текущим пользователем событие в системе по ее идентификатору.
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     * @param dto с данными для обновления события
     * @throws ConflictException если текущий пользователь не является автором события или ошибка в датах или в статусе события
     * @return EventFullDto обновленного события с актуальными данными
     */
    @Override
    @Transactional
    public EventFullDto updateEventPrivate(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.info("Запрос от пользователя ID: {} на обновление события ID: {}", userId, eventId);

        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Событие не принадлежит данному пользователю");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }

        if (dto.eventDate() != null &&
                dto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата и время, не могут быть раньше, чем через два часа от текущего момента");
        }

        // Используем маппер для обновления
        eventMapper.updateEventMap(dto, event);

        if (dto.category() != null) {
            CategoryDto categoryDto = categoryClient.getCategoryById(dto.category());
            event.setCategoryId(categoryDto.id());
        }

        // Обрабатываем stateAction отдельно
        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);

        UserShortDto userDto = userClient.findById(userId);
        CategoryDto currentCategoryDto = categoryClient.getCategoryById(updated.getCategoryId());

        log.info("Событие с ID: {} успешно обновлено в БД", updated.getId());
        return eventMapper.toFullDto(updated, userDto, currentCategoryDto);
    }

    /**
     * Публичное получение упрощенного списка событий по заданным фильтрам с поддержкой пагинации.
     * Использует QueryDSL для построения динамических критериев поиска.
     * Если сервисы не доступны, то возвращаются заглушки
     *
     * @param eventParamDto с данными для фильтрации запроса
     * @param request объект HTTP-запроса для фиксации просмотра в статистике
     * @return список EventShortDto упрощенных и отфильтрованных событий
     */
    @Override
    public List<EventShortDto> getEventsPublic(PublicEventParamDto eventParamDto, HttpServletRequest request) {
        log.info("Поиск событий пользователем по фильтрам: {}", eventParamDto);

        if (eventParamDto.rangeStart() != null && eventParamDto.rangeEnd() != null &&
                eventParamDto.rangeStart().isAfter(eventParamDto.rangeEnd())) {
            throw new ValidationException("rangeStart не может быть позже rangeEnd");
        }

        saveHit(request);

        Sort sortEventDate = Sort.unsorted();
        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("EVENT_DATE")) {
            sortEventDate = Sort.by("eventDate").ascending();
        }

        Predicate predicate = createPublicPredicate(eventParamDto);
        Pageable pageable = PageRequest.of(eventParamDto.from() / eventParamDto.size(),
                eventParamDto.size(), sortEventDate);

        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> shortsDto = mapToShortDtoList(events);

        if (eventParamDto.onlyAvailable()) {
            shortsDto = shortsDto.stream()
                    .filter(dto -> {
                        // Ищем само событие, чтобы узнать его participantLimit
                        Event originalEvent = events.stream()
                                .filter(e -> e.getId().equals(dto.getId()))
                                .findFirst()
                                .orElse(null);

                        if (originalEvent == null) return false;

                        return originalEvent.getParticipantLimit() == 0
                                || originalEvent.getParticipantLimit() > dto.getConfirmedRequests();
                    })
                    .collect(Collectors.toList());
        }

        if (eventParamDto.sort() != null && eventParamDto.sort().equalsIgnoreCase("VIEWS")) {
            shortsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        log.info("Поиск по фильтрам завершен. Найдено событий: {}", shortsDto.size());
        return shortsDto;
    }

    /**
     * Публичное получение полной информации об опубликованном событии по его идентификатору.
     *
     * @param eventId уникальный идентификатор события
     * @param request объект HTTP-запроса для фиксации просмотра в статистике
     * @throws NotFoundException если еще не опубликовано
     * @return EventFullDto подробная карточка события
     */
    @Override
    public EventFullDto getEventByIdPublic(Long eventId, HttpServletRequest request) {
        log.info("Поиск события с ID={}", eventId);

        saveHit(request);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие должно быть опубликовано");
        }

        ParamDto paramDto = new ParamDto(event.getPublishedOn(),
                LocalDateTime.now(),
                List.of(request.getRequestURI()),
                true);

        UserShortDto userDto = getUserWithFallback(event.getInitiatorId());
        CategoryDto categoryDto = getCategoryWithFallback(event.getCategoryId());
        EventFullDto fullDto = eventMapper.toFullDto(event, userDto, categoryDto);

        fullDto.setConfirmedRequests(getConfirmedRequestsWithFallback(eventId, ParticipationStatus.CONFIRMED));
        fullDto.setViews(getViews(paramDto));

        log.info("Успешно получено событие с ID = {}", eventId);
        return fullDto;
    }

    /**
     * Получение подробного списка событий по заданным фильтрам с поддержкой пагинации (запрос админа).
     * Использует QueryDSL для построения динамических критериев поиска.
     * Если сервисы не доступны, то возвращаются заглушки
     *
     * @param filter с данными для фильтрации запроса
     * @return список EventFullDto отфильтрованных событий с подробными данными
     */
    @Override
    public List<EventFullDto> searchEventsAdmin(AdminEventSearchFilter filter) {
        log.info("Поиск событий администратором по фильтрам: {}", filter);

        if (filter.rangeStart() != null && filter.rangeEnd() != null &&
                filter.rangeStart().isAfter(filter.rangeEnd())) {
            throw new ValidationException("rangeEnd не может быть раньше rangeStart");
        }

        Predicate predicate = createAdminPredicate(filter);
        Pageable pageable = PageRequest.of(filter.from() / filter.size(), filter.size());

        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventFullDto> eventsFullDto = mapToFullDtoList(events);

        log.info("Поиск администратором по фильтрам завершен. Найдено событий: {}", eventsFullDto.size());
        return eventsFullDto;
    }

    /**
     * Обновляет администратором события в системе по ее идентификатору.
     *
     * @param eventId идентификатор события
     * @param dto с данными для обновления события
     * @return EventFullDto обновленного события с актуальными данными
     */
    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Update event with ID: {}", eventId);

        Event event = getEventOrThrow(eventId);

        if (dto.eventDate() != null && dto.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не раньше, чем через час");
        }

        eventMapper.updateEventFromAdminRequest(dto, event);

        if (dto.category() != null) {
            CategoryDto categoryDto = categoryClient.getCategoryById(dto.category());
            event.setCategoryId(categoryDto.id());
        }

        if (dto.stateAction() != null) {
            switch (dto.stateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "Событие невозможно опубликовать, если оно не находится в статусе (PENDING): "
                                        + event.getState());
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    log.info("Событие с ID={} успешно опубликовано", eventId);
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Не возможно отменить событие, так как оно в статусе: PUBLISHED");
                    }
                    event.setState(EventState.CANCELED);
                    log.info("Событие с ID={} отклонено", eventId);
                }
            }
        }

        Event updated = eventRepository.save(event);

        UserShortDto userDto = userClient.findById(updated.getInitiatorId());
        CategoryDto currentCategoryDto = categoryClient.getCategoryById(updated.getCategoryId());

        log.info("Событие с ID={} успешно обновлено", updated.getId());
        return eventMapper.toFullDto(updated, userDto, currentCategoryDto);
    }

    /**
     * Возвращает событие по его идентификатору.
     *
     * @param eventId идентификатор события
     * @throws NotFoundException если событие с указанным ID не зарегистрировано в базе данных
     */
    private Event getEventOrThrow(Long eventId) {
        log.info("Получение события по ID={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найдено"));

        log.info("Получено событие с ID={}", event.getId());
        return event;
    }

    /**
     * Сохраняет статистику о запросах в stats-service.
     *
     * @param request объект HTTP-запроса для фиксации просмотра в статистике
     */
    private void saveHit(HttpServletRequest request) {
        log.info("Сохранение в сервис статистики запроса: {}", request);

        HitDto hitDto = new HitDto(
                applicationName,
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now());
        try {
            statClient.hit(hitDto);
            log.info("Статистика сохранена для URI: {}", request.getRequestURI());
        } catch (Exception e) {
            log.warn("Не удалось сохранить статистику: {}", e.getMessage());
        }
    }

    /**
     * Получает количество просмотров для события из сервиса статистики.
     * Если stats-server недоступен, метод возвращает 0L.
     *
     * @param paramDto параметры запроса статистики
     * @return Long количество просмотров или 0L при ошибке связи
     */
    private Long getViews(ParamDto paramDto) {
        log.info("Получение количества просмотров из сервиса статистики по параметрам: {}", paramDto);

        try {
            List<StatsDto> views = statClient.get(paramDto);

            return views.isEmpty() ? 0L : views.getFirst().hits();
        } catch (Exception e) {
            log.warn("stats-server недоступен при получении просмотров. Возвращаем 0L.");
            return 0L;
        }
    }

    /**
     * Пакетный сбор статистики просмотров для списка событий.
     * В случае сбоя сети (упал stats-server) возвращает пустую мапу.
     *
     * @param events список событий
     * @param unique флаг подсчета только уникальных IP-адресов
     * @return Map, где ключ — ID события, значение — количество его просмотров или пустая мапа при сбое
     */
    public Map<Long, Long> getViewsMap(List<Event> events, boolean unique) {
        log.info("Пакетный сбор статистики просмотров: events={}, unique={}", events, unique);

        try {
            String url = "/events/";
            List<String> uris = events.stream()
                    .map(event -> url + event.getId())
                    .toList();
            LocalDateTime start = events.stream()
                    .map(Event::getPublishedOn)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            List<StatsDto> stats = statClient.get(new ParamDto(start, LocalDateTime.now(), uris, unique));

            return stats.stream()
                    .filter(statsDto -> {
                        String lastPart = statsDto.uri()
                                .substring(statsDto.uri().lastIndexOf("/") + 1);
                        try {
                            Long.parseLong(lastPart);
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(
                            statsDto -> Long.parseLong(
                                    statsDto.uri().substring(statsDto.uri().lastIndexOf("/") + 1)),
                            StatsDto::hits
                    ));
        } catch (Exception e) {
            log.warn("stats-service недоступен при пакетном сборе статистики просмотров. Возвращаем пустую мапу.");
            return Map.of();
        }
    }

    /**
     * Пакетный сбор профилей пользователей по списку событий.
     * В случае сбоя сети (упал user-service) возвращает пустую мапу.
     *
     * @param events список событий для получения уникальных ID авторов
     * @return Map, где ключ — ID автора, значение — его профиль UserShortDto или пустая мапа при сбое
     */
    private Map<Long, UserShortDto> usersMap(List<Event> events) {
        log.info("Пакетный сбор профилей пользователей по списку событий:{}", events);

        List<Long> authorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        try {
            List<UserShortDto> users = userClient.getUsersByIds(authorIds);

            return users.stream()
                    .collect(Collectors.toMap(UserShortDto::id, userDto -> userDto));
        } catch (FeignException e) {
            log.error("user-service недоступен при пакетном сборе пользователей. Возвращаем пустую мапу.");
            return Map.of();
        }
    }

    /**
     * Пакетный сбор категорий по списку событий.
     * В случае сбоя сети (упал category-service) возвращает пустую мапу.
     *
     * @param events список событий для получения уникальных ID категорий
     * @return Map, где ключ — ID категории, значение — ее профиль CategoryDto или пустая мапа при сбое
     */
    private Map<Long, CategoryDto> categoriesMap(List<Event> events) {
        log.info("Пакетный сбор категорий по списку событий: {}" ,events);

        List<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .toList();

        try {
            List<CategoryDto> categories = categoryClient.getCategoriesByIds(categoryIds);

            return categories.stream()
                    .collect(Collectors.toMap(CategoryDto::id, categoryDto -> categoryDto));
        } catch (FeignException e) {
            log.error("category-service недоступен при пакетном сборе категорий. Возвращаем пустую мапу.");
            return Map.of();
        }
    }

    /**
     * Пакетный сбор количества подтвержденных заявок по списку ID событий.
     * В случае сбоя сети (упал request-service) возвращает пустую мапу.
     *
     * @param eventIds список ID событий
     * @return Map, где ключ — ID события, значение — количество заявок или пустая мапа при сбое
     */
    private Map<Long, Long> confirmedRequestsMap(List<Long> eventIds) {
        log.info("Пакетный сбор количества подтвержденных заявок по списку ID событий: {}", eventIds);

        try {
            return requestClient.getConfirmedRequestCount(eventIds).stream()
                    .collect(Collectors.toMap(ConfirmedRequestCount::eventId, ConfirmedRequestCount::count));
        } catch (FeignException e) {
            log.error("request-service недоступен при пакетном сборе заявок. Возвращаем пустую мапу.");
            return Map.of();
        }
    }

    /**
     * Возвращает результат проверки (true/false) наличия категории в системе по идентификатору
     *
     * @param categoryId идентификатор категории
     */
    @Override
    public boolean existsByCategoryId(Long categoryId) {
        log.info("Проверка наличия категории в системе по ID={}", categoryId);

        return eventRepository.existsByCategoryId(categoryId);
    }

    /**
     * Возвращает EventContractDto события из системы по его идентификатору для request-service.
     *
     * @param eventId идентификатор события
     */
    @Override
    public EventContractDto getEventByIdContract(Long eventId) {
        log.info("Получение события с ID={}", eventId);

        Event event = getEventOrThrow(eventId);

        log.info("Получено событие с ID = {}", eventId);
        return eventMapper.mapToContractDto(event);
    }

    /**
     * Возвращает EventPreviewDto события из системы по его идентификатору для CommentDto.
     *
     * @param eventId идентификатор события
     */
    @Override
    public EventPreviewDto findByIdPreview(Long eventId) {
        log.info("Получение событие с ID={}", eventId);

        Event event = getEventOrThrow(eventId);

        log.info("Событие с ID={} успешно получено", eventId);
        return eventMapper.mapToPreviewDto(event);
    }

    /**
     * Возвращает список событий из системы по их идентификаторам для CommentDto.
     *
     * @param ids со списком запрашиваемых ID
     * @return список EventPreviewDto событий согласно списку ID (может быть пустым)
     */
    @Override
    public List<EventPreviewDto> getEventPreviewByIds(List<Long> ids) {
        log.info("Поиск событий по списку ID: ids={}", ids);

        List<EventPreviewDto> eventPreviewDto = eventRepository.findAllByIdIn(ids).stream()
                .map(eventMapper::mapToPreviewDto)
                .toList();

        log.info("Поиск по списку ID завершен. Найдено событий: {}", eventPreviewDto.size());
        return eventPreviewDto;
    }

    /**
     * Возвращает упрощенный профиль пользователя по его идентификатору.
     * В случае сбоя сети (упал user-service) возвращает объект-заглушку с именем "Unknown User".
     *
     * @param userId идентификатор пользователя
     * @return UserShortDto упрощенный профиль пользователя или дефолтная заглушка при ошибке связи
     */
    private UserShortDto getUserWithFallback(Long userId) {
        log.info("Получение пользователя по ID={}", userId);

        try {
            return userClient.findById(userId);
        } catch (feign.FeignException.NotFound e) {
            throw new NotFoundException(String.format("Пользователь с ID=%d не найден", userId));
        } catch (FeignException e) {
            log.warn("user-service недоступен для получения пользователя с ID={}. Возвращаем дефолт.", userId);
            return new UserShortDto(userId, "Unknown User");
        }
    }

    /**
     * Возвращает упрощенный профиль категории по его идентификатору.
     * В случае сбоя сети (упал category-service) возвращает объект-заглушку с именем "Unknown Category".
     *
     * @param categoryId идентификатор категории
     * @return CategoryDto упрощенный профиль категории или дефолтная заглушка при ошибке связи
     */
    private CategoryDto getCategoryWithFallback(Long categoryId) {
        log.info("Получение категории по ID={}", categoryId);

        try {
            return categoryClient.getCategoryById(categoryId);
        } catch (feign.FeignException.NotFound e) {
            throw new NotFoundException(String.format("Категория с ID=%d не найдена", categoryId));
        } catch (FeignException e) {
            log.warn("category-service недоступен для получения категории с ID={}. Возвращаем дефолт.", categoryId);
            return new CategoryDto(categoryId, "Unknown Category");
        }
    }

    /**
     * Возвращает количество подтвержденных заявок для события по его идентификатору.
     * В случае сбоя сети (упал request-service) возвращает объект-заглушку с количеством заявок = OL.
     *
     * @param eventId идентификатор события
     * @param status статус заявки
     * @return Long количество подтвержденных заявок или OL при ошибке связи
     */
    private Long getConfirmedRequestsWithFallback(Long eventId, ParticipationStatus status) {
        log.info("Получение количества подтвержденных заявок по ID={} события и status={}", eventId, status);

        try {
            return requestClient.getRequestCount(eventId, status);
        } catch (FeignException e) {
            log.warn("request-service недоступен для получения количества подтвержденных заявок. Возвращаем 0L.");
            return 0L;
        }
    }

    /**
     * Создает динамический запрос QueryDSL для публичного поиска опубликованных событий (для метода getEventsPublic).
     *
     * @param eventParamDto с данными для фильтрации запроса
     * @return Predicate готового динамического запроса
     */
    private Predicate createPublicPredicate(PublicEventParamDto eventParamDto) {
        log.info("Создаем динамический запрос для публичного поиска опубликованных событий по фильтрам: {}", eventParamDto);

        QEvent event = QEvent.event;
        BooleanBuilder paramFilter = new BooleanBuilder();

        if (eventParamDto.text() != null && !eventParamDto.text().isBlank()) {
            paramFilter.and(event.annotation.containsIgnoreCase(eventParamDto.text())
                    .or(event.description.containsIgnoreCase(eventParamDto.text())));
        }

        if (eventParamDto.category() != null && !eventParamDto.category().isEmpty()) {
            paramFilter.and(event.categoryId.in(eventParamDto.category()));
        }

        if (eventParamDto.paid() != null) {
            paramFilter.and(event.paid.eq(eventParamDto.paid()));
        }

        LocalDateTime start = eventParamDto.rangeStart() != null ? eventParamDto.rangeStart() : LocalDateTime.now();
        paramFilter.and(event.eventDate.goe(start));

        if (eventParamDto.rangeEnd() != null) {
            paramFilter.and(event.eventDate.loe(eventParamDto.rangeEnd()));
        }

        paramFilter.and(event.state.eq(EventState.PUBLISHED));

        log.info("Динамический запрос успешно создан");
        return paramFilter;
    }

    /**
     * Создает динамический запрос QueryDSL для поиска событий администратором (для метода searchEventsAdmin).
     *
     * @param filter с данными для фильтрации запроса
     * @return Predicate готового динамического запроса
     */
    private Predicate createAdminPredicate(AdminEventSearchFilter filter) {
        log.info("Создаем динамический запрос для поиска событий администратором по фильтрам: {}", filter);

        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        if (filter.users() != null && !filter.users().isEmpty()) {
            predicate.and(event.initiatorId.in(filter.users()));
        }

        if (filter.states() != null && !filter.states().isEmpty()) {
            predicate.and(event.state.in(filter.states()));
        }

        if (filter.categories() != null && !filter.categories().isEmpty()) {
            predicate.and(event.categoryId.in(filter.categories()));
        }

        if (filter.rangeStart() != null) {
            predicate.and(event.eventDate.goe(filter.rangeStart()));
        }

        if (filter.rangeEnd() != null) {
            predicate.and(event.eventDate.loe(filter.rangeEnd()));
        }

        log.info("Динамический запрос для администратора успешно создан");
        return predicate;
    }

    /**
     * Преобразует список сущностей событий в список упрощённых DTO
     *
     * @param events список сущностей событий
     * @return список EventShortDto упрощенных событий
     */
    private List<EventShortDto> mapToShortDtoList(List<Event> events) {
        log.info("Получение списка EventShortDto из {} событий", events.size());

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> requestsMap = confirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsMap(events, true);
        Map<Long, UserShortDto> userDtoMap = usersMap(events);
        Map<Long, CategoryDto> categoryDtoMap = categoriesMap(events);

        List<EventShortDto> shortsDto = events.stream()
                .map(ev -> {
                    UserShortDto initiatorDto = userDtoMap.getOrDefault(ev.getInitiatorId(),
                            new UserShortDto(ev.getInitiatorId(), "Unknown User"));
                    CategoryDto categoryDto = categoryDtoMap.getOrDefault(ev.getCategoryId(),
                            new CategoryDto(ev.getCategoryId(), "Unknown Category"));
                    EventShortDto shortDto = eventMapper.toShortDto(ev, initiatorDto, categoryDto);
                    shortDto.setConfirmedRequests(requestsMap.getOrDefault(shortDto.getId(), 0L));
                    shortDto.setViews(viewsMap.getOrDefault(shortDto.getId(), 0L));
                    return shortDto;
                })
                .collect(Collectors.toList());

        log.info("Получен список упрощенный событий в количестве: {}", shortsDto.size());
        return shortsDto;
    }

    /**
     * Преобразует список сущностей событий в список подробных DTO
     *
     * @param events список сущностей событий
     * @return список EventFullDto подробных событий
     */
    private List<EventFullDto> mapToFullDtoList(List<Event> events) {
        log.info("Получение списка EventFullDto из {} событий", events.size());

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> requestsMap = confirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsMap(events, false);
        Map<Long, UserShortDto> userDtoMap = usersMap(events);
        Map<Long, CategoryDto> categoryDtoMap = categoriesMap(events);

        List<EventFullDto> eventsFullDto = events.stream()
                .map(ev -> {
                    UserShortDto initiatorDto = userDtoMap.getOrDefault(ev.getInitiatorId(),
                            new UserShortDto(ev.getInitiatorId(), "Unknown User"));
                    CategoryDto categoryDto = categoryDtoMap.getOrDefault(ev.getCategoryId(),
                            new CategoryDto(ev.getCategoryId(), "Unknown Category"));
                    EventFullDto fullDto = eventMapper.toFullDto(ev, initiatorDto, categoryDto);
                    fullDto.setConfirmedRequests(requestsMap.getOrDefault(fullDto.getId(), 0L));
                    fullDto.setViews(viewsMap.getOrDefault(fullDto.getId(), 0L));
                    return fullDto;
                })
                .toList();

        log.info("Получен список подробных событий в количестве: {}", eventsFullDto.size());
        return eventsFullDto;
    }
}