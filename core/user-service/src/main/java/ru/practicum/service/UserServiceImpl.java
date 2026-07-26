package ru.practicum.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.UserParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserPostDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.QUser;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Создает и сохраняет нового пользователя в системе.
     *
     * @param userPostDto с данными для регистрации нового пользователя
     * @return UserDto созданного пользователя с присвоенным ID
     */
    @Override
    public UserDto create(UserPostDto userPostDto) {
        log.info("Добавление нового пользователя с Email={}", userPostDto.email());

        User user = userMapper.userPostDtoToUser(userPostDto);
        User savedUser = userRepository.save(user);

        log.info("Пользователь успешно создан. ID={}, Email={}", savedUser.getId(), savedUser.getEmail());
        return userMapper.userToUserDto(savedUser);
    }

    /**
     * Возвращает список пользователей по заданным фильтрам с поддержкой пагинации.
     * Если список идентификаторов пуст или равен null, возвращаются все пользователи.
     * Использует QueryDSL для построения динамических критериев поиска.
     *
     * @param params, содержащий список ID для фильтрации 'ids', смещение 'from' и размер страницы 'size'
     * @return список UserDto отфильтрованных пользователей (может быть пустым)
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll(UserParam params) {
        log.info("Поиск пользователей по фильтрам: ids={}, from={}, size={}", params.ids(), params.from(), params.size());

        Iterable<User> users;
        int pageNumber = params.from() / params.size();
        Pageable pageSelected = PageRequest.of(pageNumber, params.size(), Sort.by("id"));

        if (params.ids() == null || params.ids().isEmpty()) {
            users = userRepository.findAll(pageSelected);
        } else {
            BooleanExpression byUserIds = QUser.user.id.in(params.ids());
            users = userRepository.findAll(byUserIds, pageSelected);
        }

        List<UserDto> usersDto = StreamSupport.stream(users.spliterator(), false)
                .map(userMapper::userToUserDto)
                .toList();

        log.info("Поиск завершен. Найдено пользователей: {}", usersDto.size());
        return usersDto;
    }

    /**
     * Удаляет пользователя из системы по его идентификатору.
     * Перед удалением проверяет существование пользователя.
     *
     * @param userId идентификатор пользователя
     * @throws NotFoundException если пользователь с указанным ID не зарегистрирован в базе данных
     */
    @Override
    public void delete(Long userId) {
        log.info("Удаление пользователя с ID={}", userId);

        User user = userRepository.findById(userId).orElseThrow(
                () ->  new NotFoundException(String.format("Пользователь с ID=%d не найден", userId)));

        userRepository.delete(user);
        log.info("Пользователь с ID={} успешно удален", userId);
    }

    /**
     * Возвращает пользователя из системы по его идентификатору.
     *
     * @param userId идентификатор пользователя
     * @throws NotFoundException если пользователь с указанным ID не зарегистрирован в базе данных
     */
    @Override
    public UserShortDto findById(Long userId) {
        log.info("Получение пользователя с ID={}", userId);

        User user = userRepository.findById(userId).orElseThrow(
                () ->  new NotFoundException(String.format("Пользователь с ID=%d не найден", userId)));

        log.info("Пользователь с ID={} успешно получен", userId);
        return userMapper.userToUserShortDto(user);
    }

    /**
     * Возвращает список пользователей согласно списку ID.
     *
     * @param ids со списком запрашиваемых ID
     * @return список UserDto пользователей согласно списку ID (может быть пустым)
     */
    @Override
    public List<UserShortDto> findAllByIdIn(List<Long> ids) {
        log.info("Поиск пользователей по списку ID: ids={}", ids);

        List<UserShortDto> usersDto = userRepository.findAllByIdIn(ids).stream()
                .map(userMapper::userToUserShortDto)
                .toList();

        log.info("Поиск по списку ID завершен. Найдено пользователей: {}", usersDto.size());
        return usersDto;
    }
}
