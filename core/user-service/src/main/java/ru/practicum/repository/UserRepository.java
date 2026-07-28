package ru.practicum.repository;

import ru.practicum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;


public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User>  {
    List<User> findAllByIdIn(List<Long> ids);
}
