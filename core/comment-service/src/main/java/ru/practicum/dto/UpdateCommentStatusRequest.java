package ru.practicum.dto;

import ru.practicum.model.CommentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCommentStatusRequest(
        @NotNull
        CommentStatus status
) {
}