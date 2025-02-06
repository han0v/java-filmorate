package ru.yandex.practicum.filmorate.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.model.Film;
import lombok.experimental.UtilityClass;

import java.util.Collections;

@UtilityClass
@Slf4j
public class FilmMapper {

    public static FilmDto toFilmDto(Film film) {
        FilmDto filmDto = new FilmDto();
        filmDto.setId(film.getId());
        filmDto.setName(film.getName());
        filmDto.setDescription(film.getDescription());
        filmDto.setReleaseDate(film.getReleaseDate());
        filmDto.setDuration(film.getDuration());
        filmDto.setMpa(film.getMpa());
        filmDto.setGenres(film.getGenres() != null ? film.getGenres() : Collections.emptyList());
        filmDto.setDirectors(film.getDirectors() != null ? film.getDirectors() : Collections.emptyList());
        log.info("FilmDto created: {}", filmDto);
        return filmDto;
    }

    public static Film toFilm(FilmDto filmDto) {
        Film film = new Film();
        film.setId(filmDto.getId());
        film.setName(filmDto.getName());
        film.setDescription(filmDto.getDescription());
        film.setReleaseDate(filmDto.getReleaseDate());
        film.setDuration(filmDto.getDuration());
        film.setMpa(filmDto.getMpa());
        film.setGenres(filmDto.getGenres());
        film.setDirectors(filmDto.getDirectors());
        return film;
    }
}