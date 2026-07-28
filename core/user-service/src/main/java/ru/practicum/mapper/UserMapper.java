package ru.practicum.mapper;

import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserPostDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {
    UserDto userToUserDto(User user);

    @Mapping(target = "id", ignore = true)
    User userPostDtoToUser(UserPostDto userPostDto);

    UserShortDto userToUserShortDto(User user);
}
