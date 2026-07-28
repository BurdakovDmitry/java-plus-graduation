package ru.practicum.dto.event;

public record EventContractDto (
        Long id,
        Long initiatorId,
        EventState state,
        Integer participantLimit,
        Boolean requestModeration
) {}
