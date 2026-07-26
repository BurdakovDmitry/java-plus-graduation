package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.model.CommentStatus;
import ru.practicum.dto.event.EventPreviewDto;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,

        String comment,

        CommentStatus status,

        EventPreviewDto event,

        UserShortDto author,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdOn,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime editedOn
) {
}
