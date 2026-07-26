package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateParticipationRequestController {
    private final ParticipationRequestService participationRequestService;

    @GetMapping
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId
    ) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return participationRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest requestUpdate
    ) {
        log.info("PATCH /users/{}/events/{}/requests", userId, eventId);
        return participationRequestService.updateRequestStatus(userId, eventId, requestUpdate);
    }
}
