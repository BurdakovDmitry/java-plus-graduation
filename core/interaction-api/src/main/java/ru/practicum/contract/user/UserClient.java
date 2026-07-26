package ru.practicum.contract.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserPostDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @PostMapping("/admin/users")
    UserDto create(@RequestBody UserPostDto user);

    @DeleteMapping("/admin/users/{userId}")
    void delete(@PathVariable("userId") Long userId);

    @GetMapping("/contract/user/{userId}")
    UserShortDto findById(@PathVariable("userId") Long userId);

    @GetMapping("/contract/user/usersIds")
    List<UserShortDto> getUsersByIds(@RequestParam("ids") List<Long> ids);
}
