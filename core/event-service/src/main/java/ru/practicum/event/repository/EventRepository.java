package ru.practicum.event.repository;

import ru.practicum.event.model.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    List<Event> findAllByIdIn(List<Long> eventIds);
}