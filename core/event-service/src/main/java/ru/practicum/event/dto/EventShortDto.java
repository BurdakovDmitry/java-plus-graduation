package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserShortDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.event.model.Location;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventShortDto {
        private Long id;
        private String annotation;
        private CategoryDto category;
        private Long confirmedRequests;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime eventDate;

        private UserShortDto initiator;
        private Boolean paid;
        private String title;
        private Double rating;
        private Location location;
}