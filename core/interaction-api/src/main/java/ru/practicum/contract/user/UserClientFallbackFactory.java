package ru.practicum.contract.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserPostDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        log.error("Сбой при вызове сервиса user-service. Причина: {}", cause.getMessage());

        return new UserClient() {
            @Override
            public UserShortDto findById(Long userId) {
                log.error("Fallback для findById: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления пользователями временно недоступен.");
            }

            @Override
            public UserDto create(UserPostDto user) {
                log.error("Fallback для create: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления пользователями временно недоступен.");
            }

            @Override
            public void delete(Long userId) {
                log.error("Fallback для delete: сервис временно недоступен");
                throw new ServiceUnavailableException("Сервер управления пользователями временно недоступен.");
            }

            @Override
            public List<UserShortDto> getUsersByIds(List<Long> ids) {
                log.error("Fallback для getUsersByIds: сервис временно недоступен");
                return List.of();
            }
        };
    }
}
