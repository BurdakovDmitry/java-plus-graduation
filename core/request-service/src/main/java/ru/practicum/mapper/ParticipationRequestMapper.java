package ru.practicum.mapper;

import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ParticipationRequestMapper {
    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "event", source = "eventId")
    ParticipationRequestDto mapToRequestDto(ParticipationRequest request);
}
