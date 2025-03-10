package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Film addFilm(Film film);

    Film updateFilm(Film film);

    Collection<Film> getAllFilms();

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    Film getFilmById(Long filmId);

    List<Film> getTopFilms(int count, Long genreId, Integer year);

    List<Film> getFilmsByDirector(Integer directorId, String sortBy);

    List<Film> getCommonFilms(Long userId, Long friendId);

    List<Film> getSearchedFilms(String query, String[] searchWords);

    void deleteFilm(Long filmId);
}
