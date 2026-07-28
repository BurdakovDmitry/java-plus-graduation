package ru.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
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

    @GetMapping("/{id}")
    public EventFullDto getPublicEventById(@PathVariable @Positive Long id,
                                           HttpServletRequest request) {
        log.info("GET /event/{}: request={}", id, request);
        return eventService.getEventByIdPublic(id, request);
    }
}
