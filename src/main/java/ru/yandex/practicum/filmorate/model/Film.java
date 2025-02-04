package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class Film {
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

