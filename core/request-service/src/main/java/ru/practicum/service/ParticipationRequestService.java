package ru.practicum.service;

import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.dto.request.ParticipationStatus;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestByUserId(Long userId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestUpdate);

    List<ConfirmedRequestCount> getConfirmedRequestCount(List<Long> eventIds);

    Long getRequestCount(Long eventId, ParticipationStatus status);
}
