package ru.practicum.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import feign.FeignException;
import ru.practicum.contract.event.EventClient;
import ru.practicum.dto.AdminCommentSearchFilter;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentSearchParams;
import ru.practicum.dto.PostCommentParam;
import ru.practicum.dto.UpdateCommentParam;
import ru.practicum.dto.UpdateCommentStatusRequest;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;
import ru.practicum.model.QComment;
import ru.practicum.repository.CommentRepository;
import ru.practicum.contract.user.UserClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotAuthorized;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    /**
     * Создает и сохраняет комментарий в системе.
     *
     * @param postCommentParam с данными для регистрации нового комментария
     * @return CommentDto созданного комментария с присвоенным ID
     */
    @Override
    @Transactional
    public CommentDto create(PostCommentParam postCommentParam) {
        log.info("Добавление нового комментария автором с ID={}", postCommentParam.author());

        Comment comment = commentMapper.postToComment(postCommentParam);
        comment.setStatus(CommentStatus.PENDING);
        Comment savedComment = commentRepository.save(comment);

        UserShortDto userDto = userClient.findById(savedComment.getAuthorId());
        EventPreviewDto eventDto = eventClient.findByIdPreview(savedComment.getEventId());

        log.info("Комментарий успешно сохранен, ID={}", savedComment.getId());
        return commentMapper.toCommentDto(savedComment, userDto, eventDto);
    }

    /**
     * Обновляет комментарий в системе.
     *
     * @param updCommentParam с данными для обновления комментария
     * @throws NotAuthorized если текущий пользователь не является автором комментария
     * @return CommentDto обновленного комментария
     */
    @Override
    @Transactional
    public CommentDto update(UpdateCommentParam updCommentParam) {
        log.info("Обновление комментария с ID={}", updCommentParam.commentId());

        Comment comment = getCommentById(updCommentParam.commentId());

        if (!comment.getAuthorId().equals(updCommentParam.author())) {
            throw new NotAuthorized("Обновить комментарий может только автор.");
        }

        comment.setComment(updCommentParam.comment());
        comment.setEditedOn(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        UserShortDto userDto = userClient.findById(savedComment.getAuthorId());
        EventPreviewDto eventDto = eventClient.findByIdPreview(savedComment.getEventId());

        log.info("Обновлен комментарий с ID={}", savedComment.getId());
        return commentMapper.toCommentDto(savedComment, userDto, eventDto);
    }

    /**
     * Удаляет комментарий из системы по его идентификатору.
     * Перед удалением проверяет, что текущий пользователь является автором.
     *
     * @param userId идентификатор пользователя
     * @param commentId идентификатор комментария
     * @throws NotAuthorized если текущий пользователь не является автором комментария
     */
    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        log.info("Удаление комментария с ID={}, пользователем с ID={}", commentId, userId);

        Comment comment = getCommentById(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new NotAuthorized("Только автор может удалить комментарий.");
        }

        commentRepository.delete(comment);
        log.info("Удален комментарий с ID={}, пользователем с ID={}", commentId, userId);
    }

    /**
     * Возвращает список комментариев текущего пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список CommentDto комментариев текущего пользователя (может быть пустым)
     */
    @Override
    public List<CommentDto> findAllByAuthor(Long userId) {
        log.info("Поиск комментариев пользователя с ID={}", userId);

        BooleanExpression byAuthorId = QComment.comment1.authorId.eq(userId);
        Iterable<Comment> comments = commentRepository.findAll(byAuthorId);
        List<Comment> commentsList = StreamSupport.stream(comments.spliterator(), false).toList();

        UserShortDto userDto = getUserWithFallback(userId);

        Map<Long, UserShortDto> userDtoMap = Map.of(userId, userDto);
        Map<Long, EventPreviewDto> eventDtoMap = eventsMap(commentsList);

        log.info("Поиск для пользователя с ID={} завершен. Найдено комментариев: {}", userId, commentsList.size());
        return commentMapper.toFullDtoList(commentsList, userDtoMap, eventDtoMap);
    }

    /**
     * Возвращает список комментариев текущего пользователя по конкретному событию.
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     * @return список CommentDto комментариев текущего пользователя по событию (может быть пустым)
     */
    @Override
    public List<CommentDto> findAllByEventAndAuthor(Long userId, Long eventId) {
        log.info("Поиск комментариев у события с ID={} пользователем с ID={}", eventId, userId);

        BooleanExpression byEventAndAuthorId = QComment.comment1.authorId.eq(userId)
                .and(QComment.comment1.eventId.eq(eventId));
        Iterable<Comment> comments = commentRepository.findAll(byEventAndAuthorId);
        List<Comment> commentsList = StreamSupport.stream(comments.spliterator(), false).toList();

        UserShortDto userDto = getUserWithFallback(userId);
        EventPreviewDto eventDto = getEventWithFallback(eventId);

        Map<Long, UserShortDto> userDtoMap = Map.of(userId, userDto);
        Map<Long, EventPreviewDto> eventDtoMap = Map.of(eventId, eventDto);

        log.info("Поиск для события с ID={} и пользователя с ID={} завершен. Найдено комментариев: {}",
                eventId, userId, commentsList.size());
        return commentMapper.toFullDtoList(commentsList, userDtoMap, eventDtoMap);
    }

    /**
     * Возвращает комментарий текущего пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @param commentId идентификатор комментария
     * @throws NotAuthorized если текущий пользователь не является автором комментария
     * @return CommentDto комментарий текущего пользователя по ID комментария
     */
    @Override
    public CommentDto findByIdAndAuthor(Long userId, Long commentId) {
        log.info("Поиск комментария с ID={} пользователем с ID={}", commentId, userId);

        Comment comment = getCommentById(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new NotAuthorized("Комментарий может запросить только его автор");
        }

        UserShortDto userDto = getUserWithFallback(userId);
        EventPreviewDto eventDto = getEventWithFallback(comment.getEventId());

        log.info("Поиск комментария с ID={} пользователем с ID={} завершен", commentId, userId);
        return commentMapper.toCommentDto(comment, userDto, eventDto);
    }

    /**
     * Возвращает список опубликованных комментариев по заданным фильтрам с поддержкой пагинации.
     * Использует QueryDSL для построения динамических критериев поиска.
     *
     * @param params, содержащий список данных для фильтрации запроса
     * @return список CommentDto отфильтрованных комментариев (может быть пустым)
     */
    @Override
    public List<CommentDto> getPublishedComments(CommentSearchParams params) {
        log.info("Поиск комментариев по фильтрам: params={}", params);

        // Проверка дат
        if (params.rangeStart() != null && params.rangeEnd() != null) {
            if (params.rangeStart().isAfter(params.rangeEnd())) {
                throw new ValidationException("rangeStart не может быть позже rangeEnd");
            }
        }

        Sort sortBy = Sort.by("createdOn").descending();
        if (params.sort() != null && params.sort().equalsIgnoreCase("asc")) {
            sortBy = Sort.by("createdOn").ascending();
        }
        Pageable pageable = PageRequest.of(params.from() / params.size(), params.size(), sortBy);

        // QueryDSL
        QComment qComment = QComment.comment1;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(qComment.status.eq(CommentStatus.PUBLISHED));

        if (params.text() != null && !params.text().isBlank()) {
            predicate.and(qComment.comment.containsIgnoreCase(params.text()));
        }
        if (params.eventId() != null) {
            predicate.and(qComment.eventId.eq(params.eventId()));
        }
        if (params.rangeStart() != null) {
            predicate.and(qComment.createdOn.goe(params.rangeStart()));
        }
        if (params.rangeEnd() != null) {
            predicate.and(qComment.createdOn.loe(params.rangeEnd()));
        }

        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();

        if (comments.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> userDtoMap = usersMap(comments);
        Map<Long, EventPreviewDto> eventDtoMap = eventsMap(comments);

        log.info("Поиск комментариев по фильтрам завершен. Найдено комментариев: {}", comments.size());
        return commentMapper.toFullDtoList(comments, userDtoMap, eventDtoMap);
    }

    /**
     * Возвращает опубликованный комментарий по идентификатору.
     *
     * @param commentId идентификатор комментария
     * @throws NotFoundException если комментарий не опубликован
     * @return CommentDto опубликованный комментарий
     */
    @Override
    public CommentDto getPublishedComment(Long commentId) {
        log.info("Поиск опубликованного комментария с ID={}", commentId);

        Comment comment = getCommentById(commentId);

        if (comment.getStatus() != CommentStatus.PUBLISHED) {
            throw new NotFoundException("Комментарий должен быть опубликован");
        }

        UserShortDto userDto = getUserWithFallback(comment.getAuthorId());
        EventPreviewDto eventDto = getEventWithFallback(comment.getEventId());

        log.info("Поиск опубликованного комментария с ID={} завершен", commentId);
        return commentMapper.toCommentDto(comment, userDto, eventDto);
    }

    /**
     * Возвращает список комментариев по заданным фильтрам с поддержкой пагинации по запросу админа.
     * Использует QueryDSL для построения динамических критериев поиска.
     *
     * @param filter, содержащий список данных для фильтрации запроса
     * @return список CommentDto отфильтрованных комментариев (может быть пустым)
     */
    @Override
    public List<CommentDto> searchComments(AdminCommentSearchFilter filter) {
        log.info("Поиск комментариев админом по фильтрам: {}", filter);

        if (filter.rangeStart() != null && filter.rangeEnd() != null
                && filter.rangeStart().isAfter(filter.rangeEnd())) {
            throw new ValidationException("rangeStart не может быть позже rangeEnd");
        }

        QComment qComment = QComment.comment1;
        BooleanBuilder predicate = new BooleanBuilder();

        Pageable pageable = PageRequest.of(filter.from() / filter.size(), filter.size());

        if (filter.text() != null && !filter.text().isBlank()) {
            predicate.and(qComment.comment.containsIgnoreCase(filter.text()));
        }

        if (filter.users() != null && !filter.users().isEmpty()) {
            predicate.and(qComment.authorId.in(filter.users()));
        }

        if (filter.eventId() != null) {
            predicate.and(qComment.eventId.eq(filter.eventId()));
        }

        if (filter.rangeStart() != null) {
            predicate.and(qComment.createdOn.goe(filter.rangeStart()));
        }

        if (filter.rangeEnd() != null) {
            predicate.and(qComment.createdOn.loe(filter.rangeEnd()));
        }

        if (filter.status() != null) {
            predicate.and(qComment.status.eq(filter.status()));
        }

        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();

        if (comments.isEmpty()) {
            return List.of();
        }

        Map<Long, UserShortDto> userDtoMap = usersMap(comments);
        Map<Long, EventPreviewDto> eventDtoMap = eventsMap(comments);

        log.info("Поиск комментариев по фильтрам админом завершен. Найдено комментариев: {}", comments.size());
        return commentMapper.toFullDtoList(comments, userDtoMap, eventDtoMap);
    }

    /**
     * Возвращает комментарий по идентификатору по запросу админа.
     *
     * @param commentId идентификатор комментария
     * @return CommentDto комментарий по ID
     */
    @Override
    public CommentDto findCommentById(Long commentId) {
        log.info("Поиск админом комментария с ID={}", commentId);

        Comment comment = getCommentById(commentId);
        UserShortDto userDto = getUserWithFallback(comment.getAuthorId());
        EventPreviewDto eventDto = getEventWithFallback(comment.getEventId());

        log.info("Поиск админом комментария с ID={} завершен", commentId);
        return commentMapper.toCommentDto(comment, userDto, eventDto);
    }

    /**
     * Обновляет статус комментария админом по его идентификатору.
     *
     * @param commentId идентификатор комментария
     * @param request с данными для обновления статуса комментария
     * @throws ConflictException если комментарий не в статусе PENDING или отклонение опубликованного комментария
     * @return CommentDto обновленный комментарий с актуальным статусом
     */
    @Override
    @Transactional
    public CommentDto updateStatusComment(Long commentId, UpdateCommentStatusRequest request) {
        log.info("Обновление комментария админом: ID={}, status={}", commentId, request.status());

        Comment comment = getCommentById(commentId);
        CommentStatus newStatus = request.status();

        if (newStatus == CommentStatus.PUBLISHED && comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно опубликовать только комментарии, находящиеся в статусе PENDING.");
        }
        if (newStatus == CommentStatus.REJECTED && comment.getStatus() == CommentStatus.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить комментарий, который уже был опубликован.");
        }

        comment.setStatus(newStatus);
        Comment saveComment = commentRepository.save(comment);

        UserShortDto userDto = getUserWithFallback(comment.getAuthorId());
        EventPreviewDto eventDto = getEventWithFallback(comment.getEventId());

        log.info("Комментарию с ID={} присвоен новый status={}", commentId, saveComment.getStatus());
        return commentMapper.toCommentDto(saveComment, userDto, eventDto);
    }

    /**
     * Удаляет комментарий админом по его идентификатору.
     *
     * @param commentId идентификатор комментария
     * @throws NotFoundException если комментарий с указанным ID не зарегистрирован в базе данных
     */
    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("Удаление комментария админом с ID={}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Комментарий с ID=%d не найден", commentId));
        }

        commentRepository.deleteById(commentId);
        log.info("Удален комментарий  с ID={}", commentId);
    }

    /**
     * Возвращает комментарий по его идентификатору.
     *
     * @param commentId идентификатор комментария
     * @throws NotFoundException если комментарий с указанным ID не зарегистрирован в базе данных
     */
    private Comment getCommentById(Long commentId) {
        log.info("Получение комментария по ID={}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID=%d не найден", commentId)));

        log.info("Получен комментарий с ID={}", comment.getId());
        return comment;
    }

    /**
     * Возвращает профиль пользователя по его идентификатору.
     * В случае сбоя сети (упал user-service) возвращает объект-заглушку с именем "Unknown User".
     *
     * @param userId идентификатор пользователя
     * @return UserShortDto профиль пользователя или дефолтная заглушка при ошибке связи
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
     * Возвращает событие по его идентификатору.
     * В случае сбоя сети (упал event-service) возвращает объект-заглушку.
     *
     * @param eventId идентификатор события
     * @return EventPreviewDto превью события или дефолтная заглушка при ошибке связи
     */
    private EventPreviewDto getEventWithFallback(Long eventId) {
        log.info("Получение события по ID={}", eventId);

        try {
            return eventClient.findByIdPreview(eventId);
        } catch (feign.FeignException.NotFound e) {
            throw new NotFoundException(String.format("Событие с ID=%d не найдено", eventId));
        } catch (FeignException e) {
            log.warn("event-service недоступен для получения события с ID={}. Возвращаем дефолт.", eventId);
            return new EventPreviewDto(
                    eventId,
                    "Unknown Annotation",
                    new CategoryDto(0L, "Unknown Category"),
                    java.time.LocalDateTime.now(),
                    new UserShortDto(0L, "Unknown Initiator"),
                    "Unknown Title"
            );
        }
    }

    /**
     * Пакетный сбор профилей пользователей по списку комментариев.
     * В случае сбоя сети (упал user-service) возвращает пустую мапу.
     *
     * @param comments список комментариев для получения уникальных ID авторов
     * @return Map, где ключ — ID автора, значение — его профиль UserShortDto или пустая мапа при сбое
     */
    private Map<Long, UserShortDto> usersMap(List<Comment> comments) {
        log.info("Пакетный сбор профилей пользователей по списку комментариев: {}", comments);
        List<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
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
     * Пакетный сбор событий по списку комментариев.
     * В случае сбоя сети (упал event-service) возвращает пустую мапу.
     *
     * @param comments список комментариев для получения уникальных ID событий
     * @return Map, где ключ — ID события, значение — его превью EventPreviewDto или пустая мапа при сбое
     */
    private Map<Long, EventPreviewDto> eventsMap(List<Comment> comments) {
        log.info("Пакетный сбор событий по списку комментариев: {}" ,comments);

        List<Long> eventsIds = comments.stream()
                .map(Comment::getEventId)
                .distinct()
                .toList();

        try {
            List<EventPreviewDto> events = eventClient.getEventPreviewByIds(eventsIds);

            return events.stream()
                    .collect(Collectors.toMap(EventPreviewDto::id, eventDto -> eventDto));
        } catch (FeignException e) {
            log.error("event-service недоступен при пакетном сборе событий. Возвращаем пустую мапу.");
            return Map.of();
        }
    }
}