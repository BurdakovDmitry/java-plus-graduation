package ru.practicum.dto;

public record UpdateCommentParam(
        Long author,
        Long commentId,
        String comment
) {
}
