package ru.practicum.contract.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.dto.event.EventPreviewDto;

import java.util.List;

@FeignClient(name = "event-service", path = "/contract/event", fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient {
    @GetMapping("/category/{catId}")
    boolean isCategory(@PathVariable("catId") Long catId);

    @GetMapping("/{eventId}")
    EventContractDto getEventByIdContract(@PathVariable("eventId") Long eventId);

    @GetMapping("/{eventId}/comment")
    EventPreviewDto findByIdPreview(@PathVariable("eventId") Long eventId);

    @GetMapping("/eventIds")
    List<EventPreviewDto> getEventPreviewByIds(@RequestParam("ids") List<Long> ids);
}
