package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.dto.request.ParticipationStatus;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/contract/request")
@RequiredArgsConstructor
public class ContractParticipationRequestController {
    private final ParticipationRequestService requestService;

    @GetMapping("/confirmed")
    List<ConfirmedRequestCount> getConfirmedRequestCount(@RequestParam List<Long> eventIds) {
        log.info("Внутренний запрос: получение количества подтвержденных заявок по списку ID событий: {}", eventIds);
        return requestService.getConfirmedRequestCount(eventIds);
    }

    @GetMapping("/count")
    Long getRequestCount(@RequestParam Long eventId,
                         @RequestParam ParticipationStatus status) {
        log.info("Внутренний запрос: получение количества заявок на событие по ID: {} и статусу: {}", eventId, status);
        return requestService.getRequestCount(eventId, status);
    }

    @GetMapping("/attendance")
    public boolean checkUserAttendance(@RequestParam Long userId,
                                       @RequestParam Long eventId) {
        log.info("Внутренний запрос проверки посещения события: userId={}, eventId={}", userId, eventId);
        return requestService.checkUserAttendedEvent(userId, eventId);
    }
}
