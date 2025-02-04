package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;
    private static final int DESCRIPTION_LENGTH = 200;
    public static final LocalDate RELEASE_DATE = LocalDate.of(1895, 12, 28);

    // Статический набор допустимых идентификаторов жанров
    private static final Set<Long> VALID_GENRE_IDS = Set.of(1L, 2L, 3L, 4L, 5L, 6L); // Пример допустимых ID жанров

    @GetMapping
    public ResponseEntity<Collection<FilmDto>> findAll() {
        log.info("Запрос на получение всех фильмов");
        Collection<Film> films = filmService.getAllFilms();
        Collection<FilmDto> filmDtos = films.stream()
                .map(FilmMapper::toFilmDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(filmDtos);
    }

    @PostMapping
    public ResponseEntity<FilmDto> create(@RequestBody FilmDto filmDto) {
        log.info("Создание нового фильма: {}", filmDto);
        Film film = FilmMapper.toFilm(filmDto);
        validateFilm(film);
        Film createdFilm = filmService.addFilm(film);
        FilmDto createdFilmDto = FilmMapper.toFilmDto(createdFilm);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFilmDto);
    }

    @PutMapping
    public ResponseEntity<FilmDto> update(@RequestBody FilmDto filmDto) {
        log.info("Обновление фильма с id = {}", filmDto.getId());
        Film existingFilm = filmService.getFilmById(filmDto.getId());
        if (existingFilm == null) {
            log.error("Фильм с id = {} не найден", filmDto.getId());
            throw new NotFoundException("Фильм с id = " + filmDto.getId() + " не найден");
        }
        Film film = FilmMapper.toFilm(filmDto);
        validateFilm(film);
        Film updatedFilm = filmService.updateFilm(film);
        FilmDto updatedFilmDto = FilmMapper.toFilmDto(updatedFilm);
        log.info("Фильм успешно обновлён: {}", updatedFilmDto);
        return ResponseEntity.ok(updatedFilmDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilmDto> getFilm(@PathVariable Long id) {
        log.info("Запрос на получение фильма с id = {}", id);
        Film film = filmService.getFilmById(id);
        if (film == null) {
            return ResponseEntity.notFound().build();
        }
        log.info("Проверка контроллера");
        log.info("Полученный фильм: {}", film);
        FilmDto filmDto = FilmMapper.toFilmDto(film);
        return ResponseEntity.ok(filmDto);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<FilmDto>> getPopularFilms(
            @RequestParam(required = false, defaultValue = "10") int count,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year) {
        log.info("Запрос на получение {} популярных фильмов с genreId = {} и year = {}", count, genreId, year);
        List<Film> popularFilms = filmService.getTopFilms(count, genreId, year);
        List<FilmDto> popularFilmDtos = popularFilms.stream()
                .map(FilmMapper::toFilmDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(popularFilmDtos);
    }

    @GetMapping("/director/{directorId}")
    public ResponseEntity<List<Film>> getFilmsByDirector(
            @PathVariable Integer directorId,
            @RequestParam String sortBy) {
        log.info("Запрос на получение фильмов режиссера {} с сортировкой по {}", directorId, sortBy);
        if (!sortBy.equals("year") && !sortBy.equals("likes")) {
            log.info("Переданы НЕДОПУСТИМЫЕ значения сортировки");
            throw new IllegalArgumentException("Некорректный параметр sortBy. Допустимые значения: year, likes");
        }
        log.info("Переданы ДОПУСТИМЫЕ значения сортировки");
        return ResponseEntity.ok(filmService.getFilmsByDirector(directorId, sortBy));
    }


    @GetMapping("/common")
    public ResponseEntity<List<FilmDto>> getCommonFilms(
            @RequestParam Long userId,
            @RequestParam Long friendId) {
        log.info("Запрос на получение общих фильмов для пользователей с id = {} и id = {}", userId, friendId);
        List<Film> commonFilms = filmService.getCommonFilms(userId, friendId);
        List<FilmDto> commonFilmDtos = commonFilms.stream()
                .map(FilmMapper::toFilmDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commonFilmDtos);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Film>> getSearchedFilm(
            @RequestParam String query,
            @RequestParam String by) {
        log.info("В контроллере запрос на поиск фильма с подстрокой {}, отсротировано по {}",
                query,
                by);
        if (query.isBlank() || by.isBlank()) {
            log.error("Ошибка: пустые параметры запроса");
            return ResponseEntity.badRequest().build();
        }
        String[] searchWords = by.toLowerCase().split(",");
        Set<String> validSearchTypes = Set.of("director", "title");
        for (String type : searchWords) {
            if (!validSearchTypes.contains(type.trim())) {
                log.error("Ошибка: недопустимое значение параметра 'by' = '{}'", by);
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(filmService.getSearchedFilms(query, searchWords));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long id) {
        log.info("Удаление фильма с id = {}", id);
        Film existingFilm = filmService.getFilmById(id);
        if (existingFilm == null) {
            log.error("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
        filmService.deleteFilm(id);
        return ResponseEntity.ok().build();
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
        if (film.getMpa().getId() > 6) {
            log.error("Ошибка валидации: Рейтинга не существует");
            throw new NotFoundException("MPA Рейтинг должен существовать");
        }
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!VALID_GENRE_IDS.contains(genre.getId())) {
                    log.error("Ошибка валидации: Жанра с id {} не существует", genre.getId());
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не существует");
                }
            }
        }
    }
}