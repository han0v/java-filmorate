package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;

@Slf4j
@Component
@Qualifier("filmDbStorage")
@Primary
public class FilmDbStorage implements FilmStorage {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final FilmRowMapper filmRowMapper;

    @Autowired
    public FilmDbStorage(NamedParameterJdbcOperations jdbcOperations, FilmRowMapper filmRowMapper) {
        this.jdbcOperations = jdbcOperations;
        this.filmRowMapper = filmRowMapper;
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO film (name, description, release_date, duration, rating_id) VALUES (:name, :description, :releaseDate, :duration, :ratingId)";

        Map<String, Object> params = new HashMap<>();
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("releaseDate", film.getReleaseDate());
        params.put("duration", film.getDuration());
        params.put("ratingId", film.getMpa().getId());

        try {
            jdbcOperations.update(sql, params);
            Long filmId = jdbcOperations.queryForObject("SELECT MAX(film_id) FROM film", params, Long.class);
            film.setId(filmId);

            if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                Set<Genre> uniqueGenres = new HashSet<>(film.getGenres());
                List<Genre> uniqueGenreList = new ArrayList<>(uniqueGenres);

                String sqlInsert = "INSERT INTO film_genre (film_id, genre_id) VALUES (:filmId, :genreId)";
                List<Map<String, Object>> batchValues = new ArrayList<>();

                for (Genre genre : uniqueGenreList) {
                    Map<String, Object> genreParams = new HashMap<>();
                    genreParams.put("filmId", filmId);
                    genreParams.put("genreId", genre.getId());
                    batchValues.add(genreParams);
                }

                // Выполняем пакетную вставку
                jdbcOperations.batchUpdate(sqlInsert, batchValues.toArray(new Map[0]));
            }

            return film;
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
            throw new RuntimeException("Произошла непредвиденная ошибка при добавлении фильма", e);
        }
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE film SET name = :name, description = :description, " +
                "release_date = :releaseDate, duration = :duration, rating_id = :ratingId " +
                "WHERE film_id = :filmId";

        Map<String, Object> params = new HashMap<>();
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("releaseDate", film.getReleaseDate());
        params.put("duration", film.getDuration());
        params.put("ratingId", film.getMpa().getId());
        params.put("filmId", film.getId());

        jdbcOperations.update(sql, params);
        updateFilmGenres(film.getId(), film.getGenres());

        return film;
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT f.*, mr.rating_mpa " +
                "FROM film f " +
                "JOIN mpa_rating mr ON f.rating_id = mr.rating_id";
        return jdbcOperations.query(sql, filmRowMapper);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (:filmId, :userId)";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);
        params.put("userId", userId);
        jdbcOperations.update(sql, params);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = :filmId AND user_id = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);
        params.put("userId", userId);
        jdbcOperations.update(sql, params);
    }

    @Override
    public Film getFilmById(Long filmId) {
        String sql = "SELECT f.*, mr.rating_mpa " +
                "FROM film f " +
                "JOIN mpa_rating mr ON f.rating_id = mr.rating_id " +
                "WHERE f.film_id = :filmId";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);

        try {
            Film film = jdbcOperations.queryForObject(sql, params, filmRowMapper);
            film.setGenres(getGenresForFilm(filmId));
            return film;
        } catch (EmptyResultDataAccessException e) {
            log.info("Фильм с id = {} не найден", filmId);
            return null;
        }
    }

    @Override
    public List<Film> getTopFilms(int count) {
        String sql = "SELECT film_id FROM film ORDER BY (SELECT COUNT(*) FROM film_likes WHERE film_id = film.film_id) DESC LIMIT :count";
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);

        List<Long> topFilmIds = jdbcOperations.query(sql, params, (rs, rowNum) -> rs.getLong("film_id"));

        return topFilmIds.stream()
                .map(this::getFilmById)
                .toList();
    }

    private void updateFilmGenres(Long filmId, List<Genre> genres) {
        String sqlDelete = "DELETE FROM film_genre WHERE film_id = :filmId";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);
        jdbcOperations.update(sqlDelete, params);

        if (genres != null && !genres.isEmpty()) {
            Set<Genre> uniqueGenres = new HashSet<>(genres);
            List<Genre> uniqueGenreList = new ArrayList<>(uniqueGenres);

            String sqlInsert = "INSERT INTO film_genre (film_id, genre_id) VALUES (:filmId, :genreId)";
            List<Map<String, Object>> batchValues = new ArrayList<>();

            for (Genre genre : uniqueGenreList) {
                Map<String, Object> genreParams = new HashMap<>();
                genreParams.put("filmId", filmId);
                genreParams.put("genreId", genre.getId());
                batchValues.add(genreParams);
            }

            jdbcOperations.batchUpdate(sqlInsert, batchValues.toArray(new Map[0]));
        }
    }

    private List<Genre> getGenresForFilm(Long filmId) {
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
}