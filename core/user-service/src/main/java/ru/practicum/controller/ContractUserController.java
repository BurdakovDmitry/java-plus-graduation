package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.service.UserService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping(path = "/contract/user")
@RequiredArgsConstructor
public class ContractUserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserShortDto findById(@PathVariable Long userId) {
        log.info("Внутренний запрос: получение пользователя по ID: {}", userId);
        return userService.findById(userId);
    }

    @GetMapping("/usersIds")
    public List<UserShortDto> getUsersByIds(@RequestParam List<Long> ids) {
        log.info("Внутренний запрос: получение пользователей по списку ID: {}", ids);
        return userService.findAllByIdIn(ids);
    }
}
