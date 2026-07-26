package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.dto.request.ParticipationStatus;

import java.time.LocalDateTime;

public record ParticipationRequestDto(
        Long id,
        Long requester,
        Long event,
        ParticipationStatus status,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime created) {
}
