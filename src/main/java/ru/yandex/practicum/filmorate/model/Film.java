package ru.yandex.practicum.filmorate.model;

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
    private List<String> genres;
    private MPARating rating;

    public enum MPARating {
        G,
        PG,
        PG_13,
        R,
        NC_17
    }
}
