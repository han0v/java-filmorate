package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/films")
public class LikeController {

    private final FilmService filmService;
    private final UserService userService;

    @PutMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long filmId, @PathVariable Long userId) {
        log.info("Пользователь с id = {} ставит лайк фильму с id = {}", userId, filmId);

        validateFilmAndUser(filmId, userId);

        filmService.addLike(filmId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long filmId, @PathVariable Long userId) {
        log.info("Пользователь с id = {} удаляет лайк к фильму с id = {}", userId, filmId);

        validateFilmAndUser(filmId, userId);

        filmService.removeLike(filmId, userId);
        return ResponseEntity.ok().build();
    }

    private void validateFilmAndUser(Long filmId, Long userId) {
        if (filmService.getFilmById(filmId) == null) {
            log.error("Ошибка валидации: Фильм с id = {} не найден.", filmId);
            throw new NotFoundException("Фильм с id = " + filmId + " не найден.");
        }
        if (userService.getUserById(userId) == null) {
            log.error("Ошибка валидации: Пользователь с id = {} не найден.", userId);
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
    }
}
