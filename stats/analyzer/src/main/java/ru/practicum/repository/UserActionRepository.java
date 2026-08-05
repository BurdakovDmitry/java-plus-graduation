package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    // Возвращает историю действий пользователя с событием
    Optional<UserAction> findByUserIdAndEventId(Integer userId, Integer eventId);

    // Возвращает всю историю действий конкретного пользователя отсортированной по времени
    List<UserAction> findAllByUserIdOrderByUpdatedAtDesc(Integer userId);

    // Возвращает список ID мероприятий, с которыми пользователь уже взаимодействовал
    @Query("""
            SELECT ua.eventId
            FROM UserAction ua
            WHERE ua.userId = :userId
            """)
    List<Integer> findInteractedEventIdsByUserId(@Param("userId") Integer userId);

    // Считает сумму максимальных весов действий для GetInteractionsCount
    @Query("""
            SELECT ua.eventId, SUM(CASE ua.actionType WHEN 'VIEW' THEN 0.4 WHEN 'REGISTER' THEN 0.8 WHEN 'LIKE' THEN 1.0 ELSE 0.0 END)
            FROM UserAction ua
            WHERE ua.eventId IN :eventIds
            GROUP BY ua.eventId
            """)
    List<Object[]> sumActionWeightsByEventIds(@Param("eventIds") List<Integer> eventIds);
}
