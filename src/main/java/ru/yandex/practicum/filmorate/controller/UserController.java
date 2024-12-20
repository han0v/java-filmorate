package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Collection<User> findAll() {
        log.info("Запрос на получение всех пользователей");
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        log.info("Запрос на получение пользователя с id = {}", id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Создание нового пользователя: {}", user);
        validateUser(user);
        User createdUser = userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser).getBody();
    }

    @PutMapping
    public User update(@RequestBody User user) {
        log.info("Обновление пользователя с id = {}", user.getId());
        return userService.updateUser(user);
    }

    @PutMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<List<User>> addFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        log.info("Пользователь с id = {} стал другом пользователя с id = {}", userId, friendId);
        if (userService.getUserById(userId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден.");
        }
        if (userService.getUserById(friendId) == null) {
            throw new IllegalArgumentException("Пользователь с id = " + friendId + " не найден.");
        }

        userService.addFriend(userId, friendId);
        List<User> friends = userService.getFriends(userId);
        return ResponseEntity.ok(friends);
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
        userService.removeFriend(userId, friendId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<User>> getFriends(@PathVariable Long userId) {
        log.info("Запрос на получение друзей пользователя с id = {}", userId);

        List<User> friends = userService.getFriends(userId);
        log.info("Друзья пользователя с id = {}: {}", userId, friends);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/{userId}/friends/common/{otherId}")
    public ResponseEntity<List<User>> getCommonFriends(@PathVariable Long userId, @PathVariable Long otherId) {
        log.info("Запрос на получение общих друзей для пользователей с id = {} и id = {}", userId, otherId);
        List<User> commonFriends = userService.getCommonFriends(userId, otherId);
        return ResponseEntity.ok(commonFriends);
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
}
