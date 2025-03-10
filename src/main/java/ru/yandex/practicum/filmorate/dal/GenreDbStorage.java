package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate jdbcOperations;

    public GenreDbStorage(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate jdbcOperations) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres";
        return jdbcTemplate.query(sql, this::mapRowToGenre);
    }

    @Override
    public Genre getGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE genre_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToGenre, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId((long) rs.getInt("genre_id"));
        genre.setName(rs.getString("genre"));
        return genre;
    }

    public List<Genre> getGenresForFilm(Long filmId) {
        String sql = "SELECT g.genre_id, g.genre " +
                "FROM film_genre fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = :filmId";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);

        return jdbcOperations.query(sql, params, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getLong("genre_id"));
            genre.setName(rs.getString("genre"));  // Добавляем имя жанра
            return genre;
        });
    }

    public Map<Long, Set<Genre>> getGenresForFilms(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = "SELECT fg.film_id, g.genre_id, g.genre " +
                "FROM film_genre fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (:filmIds)";

        Map<String, Object> params = new HashMap<>();
        params.put("filmIds", filmIds);

        List<Map<String, Object>> rows = jdbcOperations.queryForList(sql, params);

        Map<Long, Set<Genre>> filmGenresMap = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long filmId = ((Number) row.get("film_id")).longValue();
            Genre genre = new Genre();
            genre.setId(((Number) row.get("genre_id")).longValue());
            genre.setName((String) row.get("genre"));

            filmGenresMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
        }

        return filmGenresMap;
    }

}