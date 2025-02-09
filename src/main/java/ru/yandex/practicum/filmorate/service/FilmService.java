package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.event.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.event.EventOperation;
import ru.yandex.practicum.filmorate.model.event.EventType;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FilmService {
    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;
    private final EventService eventService;

    public Film addFilm(Film film) {
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film newFilm) {
        return filmStorage.updateFilm(newFilm);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void addLike(Long filmId, Long userId) {
        filmStorage.addLike(filmId, userId);

        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType(EventType.LIKE)
                .operation(EventOperation.ADD)
                .entityId(filmId)
                .build();

        eventService.addEvent(event);
    }

    public void removeLike(Long filmId, Long userId) {
        filmStorage.removeLike(filmId, userId);

        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType(EventType.LIKE)
                .operation(EventOperation.REMOVE)
                .entityId(filmId)
                .build();

        eventService.addEvent(event);
    }

    public List<Film> getTopFilms(int count, Long genreId, Integer year) {
        return filmStorage.getTopFilms(count, genreId, year);
    }

    public Film getFilmById(Long filmId) {
        return filmStorage.getFilmById(filmId);
    }

    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        return filmStorage.getFilmsByDirector(directorId, sortBy);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> getSearchedFilms(String query, String[] searchWords) {
        return filmStorage.getSearchedFilms(query, searchWords);
    }

    public void deleteFilm(Long filmId) {
        filmStorage.deleteFilm(filmId);
    }
}