package ru.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@PathVariable @Positive Long userId,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                         @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("GET /users/{}/events: from={}, size={}", userId, from, size);
        return eventService.getEventsPrivate(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable @Positive Long userId,
                                 @Valid @RequestBody NewEventDto newEventDto
    ) {
        log.info("POST /users/{}/events: newEventDto={}", userId, newEventDto);
        return eventService.addEventPrivate(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable @Positive Long userId,
                                 @PathVariable @Positive Long eventId,
                                 HttpServletRequest request) {
        log.info("GET /users/{}/events/{}: request={}", userId, eventId, request);
        return eventService.getEventByIdPrivate(userId, eventId, request.getRequestURI());
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}: updateRequest={}", userId, eventId, updateRequest);
        return eventService.updateEventPrivate(userId, eventId, updateRequest);
    }
}