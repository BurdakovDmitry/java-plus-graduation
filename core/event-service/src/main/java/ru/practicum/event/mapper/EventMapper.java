package ru.practicum.event.mapper;

import ru.practicum.dto.event.EventContractDto;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.model.Event;
import ru.practicum.dto.category.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", source = "userDto")
    @Mapping(target = "category", source = "categoryDto")
    EventShortDto toShortDto(Event event, UserShortDto userDto, CategoryDto categoryDto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "initiator", source = "userDto")
    @Mapping(target = "category", source = "categoryDto")
    EventFullDto toFullDto(Event event, UserShortDto userDto, CategoryDto categoryDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "compilations", ignore = true)
    Event toEvent(NewEventDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "compilations", ignore = true)
    void updateEventMap(UpdateEventUserRequest request, @MappingTarget Event event);

    EventContractDto mapToContractDto(Event event);

    EventPreviewDto mapToPreviewDto(Event event);
}