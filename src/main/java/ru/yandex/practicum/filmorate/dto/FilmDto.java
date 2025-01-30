package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.List;

@Data
public class FilmDto {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private int duration;
    private MpaRating mpa;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<Genre> genres;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<Director> directors;
}