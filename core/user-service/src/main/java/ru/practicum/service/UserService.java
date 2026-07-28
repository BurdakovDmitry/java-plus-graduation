package ru.practicum.service;

import ru.practicum.dto.UserParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserPostDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

public interface UserService {
    UserDto create(UserPostDto userPostDto);

    List<UserDto> findAll(UserParam params);

    void delete(Long userId);

    UserShortDto findById(Long userId);

    List<UserShortDto> findAllByIdIn(List<Long> ids);
}
