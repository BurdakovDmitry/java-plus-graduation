package ru.practicum.repository;

import ru.practicum.dto.request.ConfirmedRequestCount;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.dto.request.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    // Список своих заявок на участия в событиях
    List<ParticipationRequest> findByRequesterId(Long requesterId);

    // Проверяем наличие такого запроса
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // Количество заявок на событие
    Long countByEventIdAndStatus(Long eventId, ParticipationStatus status);

    // Список подтвержденных заявок для событий
    @Query("""
            SELECT new ru.practicum.dto.request.ConfirmedRequestCount(r.eventId, COUNT(r.id))
            FROM ParticipationRequest AS r
            WHERE r.eventId IN :eventIds AND r.status = 'CONFIRMED'
            GROUP BY r.eventId
            """)
    List<ConfirmedRequestCount> findAllConfirmedRequests(List<Long> eventIds);

    //Список заявок для события по его ID
    List<ParticipationRequest> findByEventId(Long eventId);

    //Проверка посещения события пользователем
    boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, ParticipationStatus status);
}
