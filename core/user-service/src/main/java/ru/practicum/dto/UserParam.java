package ru.practicum.dto;

import java.util.List;

public record UserParam(
        List<Long> ids,
        Integer from,
        Integer size) {
}
