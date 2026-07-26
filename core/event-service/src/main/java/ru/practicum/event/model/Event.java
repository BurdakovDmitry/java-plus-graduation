package ru.practicum.event.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.compilation.model.Compilation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import ru.practicum.dto.event.EventState;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "events")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String annotation;
    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private EventState state;
    private String title;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "initiator_id")
    private Long initiatorId;

    @Embedded
    private Location location;

    @ManyToMany(mappedBy = "events")
    @ToString.Exclude
    private List<Compilation> compilations;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return id != null && id.equals(event.getId());
    }

    @Override
    public final int hashCode() {
        return 31;
    }
}