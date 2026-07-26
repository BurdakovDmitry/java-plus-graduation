package ru.practicum.event.controller;

import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/contract/event")
public class ContractEventController {
    private final EventService eventService;

    @GetMapping("/category/{catId}")
    public boolean isCategory(@PathVariable("catId") Long catId) {
        log.info("Внутренний запрос: проверка наличия категории по ID: {}", catId);
        return eventService.existsByCategoryId(catId);
    }

    @GetMapping("/{eventId}")
    EventContractDto getEventByIdContract(@PathVariable("eventId") Long eventId) {
        log.info("Внутренний запрос: получение DTO события по ID: {}", eventId);
        return eventService.getEventByIdContract(eventId);
    }

    @GetMapping("/{eventId}/comment")
    public EventPreviewDto findByIdPreview(@PathVariable Long eventId) {
        log.info("Внутренний запрос: получение события по ID для CommentDto: {}", eventId);
        return eventService.findByIdPreview(eventId);
    }

    @GetMapping("/eventIds")
    public List<EventPreviewDto> getEventPreviewByIds(@RequestParam List<Long> ids) {
        log.info("Внутренний запрос: получение событий по списку ID для CommentDto: {}", ids);
        return eventService.getEventPreviewByIds(ids);
    }
}
