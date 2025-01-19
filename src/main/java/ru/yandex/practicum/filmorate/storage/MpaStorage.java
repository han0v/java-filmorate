package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

public interface MpaStorage {
    List<MpaRating> getAllMpaRatings();

    MpaRating getMpaRatingById(int id);
}