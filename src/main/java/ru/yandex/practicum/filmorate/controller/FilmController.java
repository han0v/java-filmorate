package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();
    private static final int DESCRIPTION_LENGTH = 200;
    public static final LocalDate RELEASE_DATE = LocalDate.of(1895, 12, 28);

    //не стал реализовывать абстрактный класс ибо поучилось некрасиво и не особо полезно

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        try {
            validateFilm(film);
            film.setId(getNextId());
            films.put(film.getId(), film);
            log.info("Создан фильм с id = {}", film.getId());
            return film;
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании фильма: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        if (newFilm.getId() == null) {
            log.error("Ошибка: Id должен быть указан при обновлении фильма");
            throw new ValidationException("Id должен быть указан");
        }

        if (films.containsKey(newFilm.getId())) {
            Film oldFilm = films.get(newFilm.getId());
            try {
                validateFilm(newFilm); // Проверяем новые значения

                // Обновляем старые значения
                oldFilm.setName(newFilm.getName());
                oldFilm.setDescription(newFilm.getDescription());
                oldFilm.setReleaseDate(newFilm.getReleaseDate());
                oldFilm.setDuration(newFilm.getDuration());

                log.info("Обновлен фильм с id = {}", newFilm.getId());
                return oldFilm;
            } catch (ValidationException e) {
                log.error("Ошибка валидации при обновлении фильма с id = {}: {}", newFilm.getId(), e.getMessage());
                throw e;
            }
        }

        log.error("Фильм с id = {} не найден для обновления", newFilm.getId());
        throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации: Название не может быть пустым");
            throw new ValidationException("Название не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > DESCRIPTION_LENGTH) {
            log.error("Ошибка валидации: Описание не может быть больше 200 символов");
            throw new ValidationException("Описание не может быть больше 200 символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(RELEASE_DATE)) {
            log.error("Ошибка валидации: Дата релиза не может быть до 28 декабря 1895 года");
            throw new ValidationException("Дата релиза не может быть до 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.error("Ошибка валидации: Продолжительность должна быть положительным числом");
            throw new ValidationException("Продолжительность должна быть положительным числом");
        }
    }

    private long getNextId() {
        return films.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0) + 1;
    }
}
