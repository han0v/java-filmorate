package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class); // Логгер для класса
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        try {
            validateUser(user);

            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(user.getLogin());
            }
            user.setId(currentId++);
            users.put(user.getId(), user);
            log.info("Создан пользователь с id = {}", user.getId());
            return user;
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании пользователя: {}", e.getMessage());
            throw e; // Пробрасываем исключение дальше
        }
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        if (newUser.getId() == null) {
            log.error("Ошибка: Id должен быть указан при обновлении пользователя");
            throw new ValidationException("Id должен быть указан");
        }

        if (users.containsKey(newUser.getId())) {
            User existingUser = users.get(newUser.getId());
            try {
                validateUser(newUser);

                if (newUser.getName() == null || newUser.getName().isBlank()) {
                    newUser.setName(newUser.getLogin());
                } else {
                    existingUser.setName(newUser.getName());
                }

                existingUser.setEmail(newUser.getEmail());
                existingUser.setLogin(newUser.getLogin());
                existingUser.setBirthday(newUser.getBirthday());

                log.info("Обновлен пользователь с id = {}", newUser.getId());
                return existingUser;
            } catch (ValidationException e) {
                log.error("Ошибка валидации при обновлении пользователя с id = {}: {}", newUser.getId(), e.getMessage());
                throw e; // Пробрасываем исключение дальше
            }
        }

        log.error("Пользователь с id = {} не найден для обновления", newUser.getId());
        throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    // Метод для валидации пользователя
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
