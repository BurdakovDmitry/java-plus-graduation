package ru.practicum.event.service;

import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.event.dto.AdminEventSearchFilter;
import ru.practicum.dto.event.EventContractDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.PublicEventParamDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.model.Event;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface EventService {

    List<EventShortDto> getEventsPrivate(Long userId, Integer from, Integer size);

    EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto);

    EventFullDto getEventByIdPrivate(Long userId, Long eventId, String url);

    EventFullDto updateEventPrivate(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventShortDto> getEventsPublic(PublicEventParamDto paramDto, HttpServletRequest request);

    EventFullDto getEventByIdPublic(Long id, HttpServletRequest request);

    List<EventFullDto> searchEventsAdmin(AdminEventSearchFilter filter);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto);

    Map<Long, Long> getViewsMap(List<Event> events, boolean unique);

    boolean existsByCategoryId(Long categoryId);

    EventContractDto getEventByIdContract(Long eventId);

    EventPreviewDto findByIdPreview(Long eventId);

    List<EventPreviewDto> getEventPreviewByIds(List<Long> ids);
}