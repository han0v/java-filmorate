package ru.yandex.practicum.filmorate.dal.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Component
public class FilmRowMapper implements RowMapper<Film> {

    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        // Проверка на наличие данных рейтинга
        MpaRating mpaRating = new MpaRating();
        mpaRating.setId(rs.getLong("rating_id"));
        mpaRating.setName(rs.getString("rating_MPA") != null ? rs.getString("rating_MPA") : "Неизвестно"); // Проверка на null
        film.setMpa(mpaRating);

        // Оставляем пустые списки, жанры и режиссеры будут загружены отдельно
        film.setGenres(new ArrayList<>());
        film.setDirectors(new ArrayList<>());

        return film;
    }
}