package ewm.user.mapper;

import ewm.user.dto.UserDto;
import ewm.user.dto.UserPostDto;
import ewm.user.dto.UserShortDto;
import ewm.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto userToUserDto(User user);

    @Mapping(target = "id", ignore = true)
    User userPostDtoToUser(UserPostDto userPostDto);

    UserShortDto userToUserShortDto(User user);
}
