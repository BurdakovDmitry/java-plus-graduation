package ru.practicum.service;

import client.StatClient;
import ru.practicum.contract.event.EventClient;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.collector.ActionType;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.dto.event.EventState;
import feign.FeignException;
import ru.practicum.contract.user.UserClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.dto.request.ParticipationStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;
    private final StatClient statClient;

    /**
     * Возвращает список заявок текущего пользователя на участие в чужих событиях.
     *
     * @param userId идентификатор пользователя
     * @return список ParticipationRequestDto заявок пользователя (может быть пустым)
     */
    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        log.info("Получение заявок пользователя с ID={}", userId);

        checkUser(userId);

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.info("Получен список заявок пользователя с ID={}", userId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    /**
     * Добавляет заявку текущего пользователя на участие в событии.
     * Отправляем информацию о регистрации 'REGISTER' по gRPC в Collector
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     * @throws ConflictException если заявка уже существует или собственное событие, или событие не опубликовано, или достигнут лимит
     * @return ParticipationRequestDto созданной заявки с присвоенным ID
     */
    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Добавление заявки от пользователя с ID={} на событие с ID={}", userId, eventId);

        checkUser(userId);
        EventContractDto event = eventClient.getEventByIdContract(eventId);

        ParticipationRequest request = new ParticipationRequest();
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на участие уже существует");
        }

        if (event.initiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в нем");
        }

        if (!event.state().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие еще не опубликовано");
        }

        if (event.participantLimit() != 0 && event.participantLimit() <= confirmedRequests) {
            throw new ConflictException("Достигнут лимит участников для этого события");
        }

        if (event.requestModeration() == false || event.participantLimit() == 0) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationStatus.PENDING);
        }

        request.setRequesterId(userId);
        request.setEventId(eventId);

        ParticipationRequest saveRequest = requestRepository.save(request);

        statClient.sendUserActionToCollector(request.getRequesterId(), request.getEventId(), ActionType.REGISTER);

        log.info("Заявка добавлена от пользователя с ID={} на событие с ID={}", userId, eventId);
        return requestMapper.mapToRequestDto(saveRequest);
    }

    /**
     * Отменяет заявку ее инициатором.
     *
     * @param userId идентификатор пользователя
     * @param requestId идентификатор заявки
     * @throws NotFoundException если заявка с указанным ID не зарегистрирована в базе данных
     * @throws ValidationException если это чужая заявка
     * @return ParticipationRequestDto заявку со статусом 'CANCELED'
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки с ID={} пользователем с ID={}", requestId, userId);

        checkUser(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с ID=" + requestId + " не найдена"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ValidationException("Вы можете отменить только свой собственный запрос");
        }

        request.setStatus(ParticipationStatus.CANCELED);
        ParticipationRequest saveRequest = requestRepository.save(request);

        log.info("Заявка с ID={} успешно отменена пользователем с ID={}", requestId, userId);
        return requestMapper.mapToRequestDto(saveRequest);
    }

    /**
     * Возвращает список заявок на участие в событии текущего пользователя (для инициатора этого события).
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     * @throws NotFoundException если текущий пользователь не является инициатором события
     * @return список ParticipationRequestDto заявок пользователей на участие в событии (может быть пустым)
     */
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Просмотр заявок к событию с ID={} создателем с ID={}", eventId, userId);

        checkUser(userId);


        EventContractDto event = eventClient.getEventByIdContract(eventId);

        if (!event.initiatorId().equals(userId)) {
            throw new NotFoundException("Событие с ID=" + eventId + " не найдено для пользователя с ID=" + userId);
        }

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        log.info("Получен список заявок на участие в событии с ID={} создателем с ID={}", eventId, userId);
        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();

    }

    /**
     * Обновляет статус (подтверждение/отклонение) заявок на участие в событии пользователем.
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     * @param requestUpdate с данными для обновления статуса заявки
     * @throws NotFoundException если текущий пользователь не является инициатором события
     * @throws ConflictException если событие не в статусе ожидания или достигнут лимит
     * @throws ValidationException если заявка не относится к этому событию
     * @return результат EventRequestStatusUpdateResult обновления статуса заявок для события
     */
    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestUpdate) {
        log.info("Обновление статуса заявки для события с ID={} пользователем с ID={}", eventId, userId);

        checkUser(userId);

        EventContractDto event = eventClient.getEventByIdContract(eventId);

        if (!event.initiatorId().equals(userId)) {
            throw new NotFoundException("Событие с ID=" + eventId + " не найдена для пользователя с ID=" + userId);
        }

        // Получаем список заявок
        List<ParticipationRequest> requests = requestRepository.findAllById(requestUpdate.requestIds());

        // Создаём списки для результатов
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        // Текущее количество подтверждённых заявок
        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        if (event.participantLimit() != 0 && confirmedRequests >= event.participantLimit()) {
            throw new ConflictException("Достигнут лимит участников для этого события");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEventId().equals(eventId)) {
                throw new ValidationException("Заявка не относится к этому событию");
            }
            if (request.getStatus() != ParticipationStatus.PENDING) {
                throw new ConflictException("Событие должно быть в статусе PENDING");
            }

            if ("REJECTED".equals(requestUpdate.status())) {
                request.setStatus(ParticipationStatus.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if ("CONFIRMED".equals(requestUpdate.status())) {
                if (event.participantLimit() == 0 || confirmedRequests < event.participantLimit()) {
                    request.setStatus(ParticipationStatus.CONFIRMED);
                    confirmed.add(requestMapper.mapToRequestDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(ParticipationStatus.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);

        log.info("Обновлён статус заявок для события с ID={} пользователем с ID={}", eventId, userId);
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    /**
     * Возвращает список подтвержденных заявок на участие в событиях.
     *
     * @param eventIds список идентификаторов событий
     * @return список ConfirmedRequestCount подтвержденных заявок (может быть пустым)
     */
    @Override
    public List<ConfirmedRequestCount> getConfirmedRequestCount(List<Long> eventIds) {
        log.info("Получение подтвержденных заявок по списку ID событий: {}", eventIds);
        return requestRepository.findAllConfirmedRequests(eventIds);
    }

    /**
     * Возвращает количество заявок на участие в событии в зависимости от статуса заявки.
     *
     * @param eventId идентификатор события
     * @param status статус заявки для этого события
     * @return количество заявок (может быть пустым)
     */
    @Override
    public Long getRequestCount(Long eventId, ParticipationStatus status) {
        log.info("Получение количества заявок для события с ID={} и статусом заявки={}", eventId, status);
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    /**
     * Проверяет посещение события (статус 'CONFIRMED') пользователем.
     *
     * @param userId идентификатор пользователя
     * @param eventId идентификатор события
     */
    @Override
    public boolean checkUserAttendedEvent(Long userId, Long eventId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, ParticipationStatus.CONFIRMED);
    }

    /**
     * Проверяет существование пользователя по его идентификатору.
     *
     * @param userId идентификатор пользователя
     * @throws NotFoundException если пользователь с указанным ID не зарегистрирован в базе данных
     */
    private void checkUser(Long userId) {
        log.info("Проверка наличия пользователя с ID={} в БД", userId);

        try {
            userClient.findById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(String.format("Пользователь с ID=%d не найден", userId));
        }
    }
}
