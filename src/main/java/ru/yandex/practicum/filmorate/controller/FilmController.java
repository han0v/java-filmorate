package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;
    private final UserService userService;
    private static final int DESCRIPTION_LENGTH = 200;
    public static final LocalDate RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @GetMapping
    public ResponseEntity<Collection<Film>> findAll() {
        log.info("Запрос на получение всех фильмов");
        Collection<Film> films = filmService.getAllFilms();
        return ResponseEntity.ok(films);
    }

    @PostMapping
    public ResponseEntity<Film> create(@RequestBody Film film) {
        log.info("Создание нового фильма: {}", film);
        validateFilm(film);
        Film createdFilm = filmService.addFilm(film);
        log.info("Фильм успешно создан: {}", createdFilm);
        return ResponseEntity.ok(createdFilm);
    }

    @PutMapping
    public ResponseEntity<Film> update(@RequestBody Film newFilm) {
        log.info("Обновление фильма с id = {}", newFilm.getId());
        Film updatedFilm = filmService.updateFilm(newFilm);
        log.info("Фильм успешно обновлён: {}", updatedFilm);
        return ResponseEntity.ok(updatedFilm);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilm(@PathVariable Long id) {
        log.info("Запрос на получение фильма с id = {}", id);
        Film film = filmService.getFilmById(id);
        return film != null ? ResponseEntity.ok(film) : ResponseEntity.notFound().build();
    }


    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getPopularFilms(
            @RequestParam(required = false, defaultValue = "10") int count) {
        log.info("Запрос на получение {} популярных фильмов", count);
        List<Film> popularFilms = filmService.getTopFilms(count);
        return ResponseEntity.ok(popularFilms);
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
}
