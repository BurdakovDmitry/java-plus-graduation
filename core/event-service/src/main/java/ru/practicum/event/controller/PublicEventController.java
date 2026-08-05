package ru.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PublicEventParamDto;
import ru.practicum.event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getPublicEvents(@Valid PublicEventParamDto param,
                                              HttpServletRequest request) {
        log.info("GET /event: param={}, request={}", param, request);
        return eventService.getEventsPublic(param, request);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getPublicEventById(@PathVariable @Positive Long eventId,
                                           @RequestHeader("X-EWM-USER-ID") @Positive Long userId,
                                           HttpServletRequest request) {
        log.info("GET /event/{}: userId={}, request={}", eventId, userId, request);
        return eventService.getEventByIdPublic(eventId, userId, request);
    }

    @GetMapping("/recommendations")
    public List<EventFullDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") @Positive Long userId,
                                                 @RequestParam(defaultValue = "10") @Positive int maxResults) {
        log.info("GET /event/recommendations: userId={}, maxResults={}", userId, maxResults);
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(@PathVariable @Positive Long eventId,
                          @RequestHeader("X-EWM-USER-ID") @Positive Long userId) {
        log.info("PUT /events/{}/like: userId={}", eventId, userId);
        eventService.likeEvent(eventId, userId);
    }
}
