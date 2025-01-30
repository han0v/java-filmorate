package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Qualifier("filmDbStorage")
@Repository
public class FilmDbStorage implements FilmStorage {

    @Autowired
    private final NamedParameterJdbcOperations jdbcOperations;
    private final FilmRowMapper filmRowMapper;

    @Autowired
    public FilmDbStorage(NamedParameterJdbcOperations jdbcOperations, FilmRowMapper filmRowMapper) {
        this.jdbcOperations = jdbcOperations;
        this.filmRowMapper = filmRowMapper;
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO film (name, description, release_date, duration, rating_id) " +
                "VALUES (:name, :description, :releaseDate, :duration, :ratingId)";
        Map<String, Object> params = new HashMap<>();
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("releaseDate", film.getReleaseDate());
        params.put("duration", film.getDuration());
        params.put("ratingId", film.getMpa().getId());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcOperations.update(sql, new MapSqlParameterSource(params), keyHolder, new String[]{"film_id"});
            Number filmId = keyHolder.getKey();
            log.debug("Полученный filmId: {}", filmId);
            if (filmId == null) {
                throw new RuntimeException("Ошибка при получении ID фильма");
            }
            film.setId((Long) filmId);
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
            //обновляем результирующую таблицу
            if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
                insertDirectorAndFilms(film);
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
        //обновляем результирующую таблицу
        insertDirectorAndFilms(film);

        return film;
    }

    private void insertDirectorAndFilms(Film film) {
        // Вставка режиссеров
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            String sqlInsertDirectors = "INSERT INTO films_directors (film_id, director_id) VALUES (:filmId, :directorId)";
            List<Map<String, Object>> batchValuesDirectors = new ArrayList<>();

            for (Director director : film.getDirectors()) {
                log.info("Вставляем режиссера с id: {}", director.getId());
                Map<String, Object> directorParams = new HashMap<>();
                directorParams.put("filmId", film.getId());
                directorParams.put("directorId", director.getId());
                batchValuesDirectors.add(directorParams);
            }
            log.info("Запуск пакетной вставки режиссеров для фильма с id: {}", film.getId());
            // Выполняем пакетную вставку режиссеров
            jdbcOperations.batchUpdate(sqlInsertDirectors, batchValuesDirectors.toArray(new Map[0]));
        }
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.*, mr.rating_mpa " +
                "FROM film f " +
                "JOIN mpa_rating mr ON f.rating_id = mr.rating_id";
        try {
            // Получаем все фильмы
            List<Film> films = jdbcOperations.query(sql, new HashMap<>(), filmRowMapper);
            // Получаем жанры и режиссеров для каждого фильма
            for (Film film : films) {
                Long filmId = film.getId();
                // Получаем жанры для каждого фильма
                film.setGenres(getGenresForFilm(filmId));
                // Получаем режиссеров для каждого фильма
                String directorsSql = "SELECT d.director_id, d.name " +
                        "FROM directors d " +
                        "JOIN films_directors fd ON d.director_id = fd.director_id " +
                        "WHERE fd.film_id = :filmId";
                Map<String, Object> directorParams = new HashMap<>();
                directorParams.put("filmId", filmId);
                List<Director> directors = jdbcOperations.query(directorsSql, directorParams, (rs, rowNum) -> {
                    Director director = new Director();
                    director.setId(rs.getInt("director_id"));
                    director.setName(rs.getString("name"));
                    return director;
                });
                film.setDirectors(directors);  // Устанавливаем список режиссеров в объект Film
            }
            return films;
        } catch (EmptyResultDataAccessException e) {
            log.info("Фильмы не найдены");
            return Collections.emptyList();
        }
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
            // Получаем фильм
            Film film = jdbcOperations.queryForObject(sql, params, filmRowMapper);

            // Получаем жанры фильма
            film.setGenres(getGenresForFilm(filmId));

            // Получаем список режиссеров фильма
            String directorsSql = "SELECT d.director_id, d.name " +
                    "FROM directors d " +
                    "JOIN films_directors fd ON d.director_id = fd.director_id " +
                    "WHERE fd.film_id = :filmId";
            Map<String, Object> directorParams = new HashMap<>();
            directorParams.put("filmId", filmId);
            List<Director> directors = jdbcOperations.query(directorsSql, directorParams, (rs, rowNum) -> {
                Director director = new Director();
                director.setId(rs.getInt("director_id"));
                director.setName(rs.getString("name"));
                return director;
            });

            film.setDirectors(directors);  // Устанавливаем список режиссеров в объект Film

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

    @Override
    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        log.info("Запущен метод по получению фильмов режиссера с id = {}", directorId);
        String sqlFilmsByYear = "SELECT fd.director_id, fd.film_id " +
                "FROM films_directors as fd " +
                "WHERE fd.director_id = :director_id";
        MapSqlParameterSource namedParameters = new MapSqlParameterSource("director_id", directorId);
        log.info("Вот такой передаётся запрос: {}", sqlFilmsByYear);
        List<Integer> filmsIds = jdbcOperations.query(sqlFilmsByYear, namedParameters, (rs, rowNum) -> {
            return rs.getInt("film_id");
        });
        log.info("Списки айдишников: {}", filmsIds);
        log.info("Попытка получить список всех фильмов:");
        List<Film> films = getFilmsById(filmsIds);
        log.info("Тот метод отработал");
        log.info("Его результаты(неотсортированные фильмы): {}", films);
        //Сортирока по году
        if (sortBy.equals("year")) {
            films = films.stream()
                    .sorted(Comparator.comparing(Film::getReleaseDate))  // Сортировка по дате релиза
                    .collect(Collectors.toList());
            log.info("Отсортированные фильмы: {}", films);
            return films;
        } else {
            //Сортировка по лайкам
            //Другого значение в softBY не может быть. Проверка в фильм контроллере
            log.info("Попытка получить количество лайков для фильмов");
            // Получаем количество лайков для каждого фильма
            String sqlLikesCount = "SELECT film_id, COUNT(*) AS like_count " +
                    "FROM film_likes " +
                    "WHERE film_id IN (:filmIds) " +
                    "GROUP BY film_id";

            MapSqlParameterSource likeParams = new MapSqlParameterSource("filmIds", filmsIds);
            List<Map<String, Object>> likesResult = jdbcOperations.queryForList(sqlLikesCount, likeParams);

            // Создаём мапу для фильмов и их количества лайков
            Map<Integer, Integer> filmLikesMap = new HashMap<>();
            for (Map<String, Object> like : likesResult) {
                Integer filmId = (Integer) like.get("film_id");
                Integer likeCount = ((Long) like.get("like_count")).intValue();
                filmLikesMap.put(filmId, likeCount);
            }

            // Сортируем фильмы по количеству лайков
            films = films.stream()
                    .sorted((film1, film2) -> Integer.compare(filmLikesMap.getOrDefault(film2.getId().intValue(), 0),
                            filmLikesMap.getOrDefault(film1.getId().intValue(), 0)))  // Сортировка по убыванию лайков
                    .collect(Collectors.toList());
            log.info("Отсортированные фильмы по лайкам: {}", films);
            return films;
        }
    }

    //вспомогательный метод
    private List<Film> getFilmsById(List<Integer> filmsIds) {
        log.info("Запущен метод по получению всех фильмов по множественным id");
        List<Film> films = new ArrayList<>();
        for (Integer id : filmsIds) {
            films.add(getFilmById(Long.valueOf(id)));
        }
        log.info("Список всех фильмов: {}", films);
        // Получаем список фильмов
        return films;
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