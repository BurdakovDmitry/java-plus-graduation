package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityCompositeKey;

import java.util.List;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityCompositeKey> {
    // Ищет пары подобия для мероприятия с сортировкой (ID может быть как на позиции A, так и на позиции B)
    @Query("""
            SELECT es
            FROM EventSimilarity es
            WHERE es.eventA = :eventId OR es.eventB = :eventId
            ORDER BY es.score DESC""")
    List<EventSimilarity> findAllSimilarToEventOrderByScoreDesc(@Param("eventId") Integer eventId);

    // Ищет пары подобия для мероприятия по списку ID
    List<EventSimilarity> findByEventAInOrEventBInOrderByScoreDesc(List<Integer> eventsA, List<Integer> eventsB);
}
