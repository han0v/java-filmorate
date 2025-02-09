package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.event.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Collection<UserDto>> findAll() {
        log.info("Запрос на получение всех пользователей");
        Collection<User> users = userService.getAllUsers();
        Collection<UserDto> userDtos = users.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        log.info("Запрос на получение пользователя с id = {}", id);
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserDto userDto = UserMapper.toUserDto(user);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{id}/feed")
    public List<Event> getUserFeed(@PathVariable Long id) {
        return eventService.getUserFeed(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserDto userDto) {
        log.info("Создание нового пользователя: {}", userDto);
        User user = UserMapper.toUser(userDto);
        validateUser(user);
        User createdUser = userService.addUser(user);
        UserDto createdUserDto = UserMapper.toUserDto(createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUserDto);
    }

    @PutMapping
    public ResponseEntity<UserDto> update(@RequestBody UserDto userDto) {
        log.info("Обновление пользователя с id = {}", userDto.getId());
        User user = UserMapper.toUser(userDto);
        if (userService.getUserById(user.getId()) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + user.getId() + " не найден.");
        }
        validateUser(user);
        User updatedUser = userService.updateUser(user);
        UserDto updatedUserDto = UserMapper.toUserDto(updatedUser);
        return ResponseEntity.ok(updatedUserDto);
    }

    @PutMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<List<UserDto>> addFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        log.info("Пользователь с id = {} стал другом пользователя с id = {}", userId, friendId);
        if (userService.getUserById(userId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден.");
        }
        if (userService.getUserById(friendId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + friendId + " не найден.");
        }
        userService.addFriend(userId, friendId);
        List<User> friends = userService.getFriends(userId);
        List<UserDto> friendDtos = friends.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(friendDtos);
    }

    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        log.info("Пользователь с id = {} удалил пользователя с id = {} из друзей", userId, friendId);
        if (userService.getUserById(userId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден.");
        }
        if (userService.getUserById(friendId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + friendId + " не найден.");
        }
        validateUserId(userId);
        validateUserId(friendId);
        userService.removeFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserDto>> getFriends(@PathVariable Long userId) {
        log.info("Запрос на получение друзей пользователя с id = {}", userId);
        if (userService.getUserById(userId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден.");
        }
        List<User> friends = userService.getFriends(userId);
        List<UserDto> friendDtos = friends.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(friendDtos);
    }

    @GetMapping("/{userId}/friends/common/{otherId}")
    public ResponseEntity<List<UserDto>> getCommonFriends(@PathVariable Long userId, @PathVariable Long otherId) {
        log.info("Запрос на получение общих друзей для пользователей с id = {} и id = {}", userId, otherId);
        List<User> commonFriends = userService.getCommonFriends(userId, otherId);
        List<UserDto> commonFriendDtos = commonFriends.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commonFriendDtos);
    }

    @GetMapping("/{userId}/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(@PathVariable Long userId) {
        validateUserId(userId);
        log.info("Запрос рекомендаций для пользователя с id = {}", userId);
        List<Film> recommendedFilms = userService.getRecommendations(userId);
        List<FilmDto> recommendedFilmDtos = recommendedFilms.stream()
                .map(FilmMapper::toFilmDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recommendedFilmDtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Удаление пользователя с id = {}", id);
        validateUserId(id);
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }


    private void validateUser(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            log.error("Некорректный email: {}", user.getEmail());
            throw new ValidationException("Некорректный email");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error("Логин не может быть пустым или содержать пробелы: {}", user.getLogin());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Дата рождения не может быть в будущем или пустой: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем или пустой");
        }
    }

    private void validateUserId(Long userId) {
        if (userService.getUserById(userId) == null) {
            log.error("Пользователь с id = {} не найден.", userId);
            throw new ValidationException("Пользователь с id = " + userId + " не найден.");
        }
    }
}