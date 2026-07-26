package ru.practicum.compilation.dto;

import ru.practicum.event.dto.EventShortDto;

import java.util.List;

public record CompilationDto(
        List<EventShortDto> events,
        Long id,
        Boolean pinned,
        String title) {
}
