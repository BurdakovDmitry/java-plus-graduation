package ru.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

@Table(name = "event_similarity")
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EventSimilarityCompositeKey.class) // Подключаем составной ключ
public class EventSimilarity {
    @Id
    @Column(name = "event_a_id", nullable = false)
    private Integer eventA;

    @Id
    @Column(name = "event_b_id", nullable = false)
    private Integer eventB;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventSimilarity that)) return false;
        return Objects.equals(eventA, that.eventA) && Objects.equals(eventB, that.eventB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventA, eventB);
    }
}
