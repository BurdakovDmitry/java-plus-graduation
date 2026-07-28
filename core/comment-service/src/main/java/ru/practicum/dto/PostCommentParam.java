package ru.practicum.dto;

public record PostCommentParam(
        Long author,
        Long event,
        String comment
) {
}
