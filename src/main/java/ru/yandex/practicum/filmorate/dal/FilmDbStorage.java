package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Qualifier("filmDbStorage")
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final NamedParameterJdbcOperations jdbcOperations;
    private final FilmRowMapper filmRowMapper;
    private final GenreDbStorage genreDbStorage;
    private final DirectorDbStorage directorDbStorage;
    private final MpaDbStorage mpaDbStorage;


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
        removeDirectorsFromFilm(film.getId());
        insertDirectorAndFilms(film);

        film.setGenres(genreDbStorage.getGenresForFilm(film.getId()));

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
            jdbcOperations.batchUpdate(sqlInsertDirectors, batchValuesDirectors.toArray(new Map[0]));
        }
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.*, mr.rating_mpa " +
                "FROM film f " +
                "JOIN mpa_rating mr ON f.rating_id = mr.rating_id";
        try {
            List<Film> films = jdbcOperations.query(sql, new HashMap<>(), filmRowMapper);
            for (Film film : films) {
                Long filmId = film.getId();
                film.setGenres(genreDbStorage.getGenresForFilm(filmId));
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
                film.setDirectors(directors);
            }
            return films;
        } catch (EmptyResultDataAccessException e) {
            log.info("Фильмы не найдены");
            return Collections.emptyList();
        }
    }


    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id) VALUES (:filmId, :userId)";
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
            film.setGenres(genreDbStorage.getGenresForFilm(filmId));

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

            film.setDirectors(directorDbStorage.getDirectorsForFilm(film.getId()));
            return film;
        } catch (EmptyResultDataAccessException e) {
            log.info("Фильм с id = {} не найден", filmId);
            return null;
        }
    }


    @Override
    public List<Film> getTopFilms(int count, Long genreId, Integer year) {
        String sql = "SELECT f.film_id " +
                "FROM film f " +
                "LEFT JOIN film_likes fl ON f.film_id = fl.film_id " +
                (genreId != null ? "JOIN film_genre fg ON f.film_id = fg.film_id " : "") +
                "WHERE 1=1 " + // упрощает динамическую генерацию условий
                (genreId != null ? "AND fg.genre_id = :genreId " : "") +
                (year != null ? "AND YEAR(f.release_date) = :year " : "") +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(fl.user_id) DESC " +
                "LIMIT :count";
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);
        if (genreId != null) {
            params.put("genreId", genreId);
        }
        if (year != null) {
            params.put("year", year);
        }
        List<Long> topFilmIds = jdbcOperations.query(sql, params, (rs, rowNum) -> rs.getLong("film_id"));
        return topFilmIds.stream()
                .map(this::getFilmById)
                .toList();


    }

    @Override
    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        log.info("Запущен метод по получению фильмов режиссера с id = {}", directorId);

        if (!isDirectorExist(directorId)) {
            log.warn("Режиссер с id = {} не найден", directorId);
            throw new NotFoundException("Режиссер с id " + directorId + " не найден");
        }
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
//            Set<Genre> uniqueGenres = new HashSet<>(genres);
            List<Genre> uniqueGenres = genres.stream()
                    .distinct()
                    .collect(Collectors.toList());
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


    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.*, COALESCE(fl.likes_count, 0) AS likes_count, mr.rating_mpa " +
                "FROM film f " +
                "JOIN film_likes l1 ON f.film_id = l1.film_id " +
                "JOIN film_likes l2 ON f.film_id = l2.film_id " +
                "LEFT JOIN (" +
                "    SELECT film_id, COUNT(*) AS likes_count " +
                "    FROM film_likes " +
                "    GROUP BY film_id" +
                ") fl ON f.film_id = fl.film_id " +
                "JOIN MPA_RATING mr ON f.rating_id = mr.rating_id " +
                "WHERE l1.user_id = :userId " +
                "AND l2.user_id = :friendId " +
                "ORDER BY likes_count DESC";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("friendId", friendId);
        try {
            List<Film> films = jdbcOperations.query(sql, params, filmRowMapper);

            for (Film film : films) {
                film.setGenres(genreDbStorage.getGenresForFilm(film.getId()));
            }
            return films;
        } catch (Exception e) {
            log.error("Ошибка при выполнении SQL-запроса: ", e);
            throw e;
        }
    }

    @Override
    public List<Film> getSearchedFilms(String query, String[] searchColumns) {
        log.info("Вызван метод в хранилище по желаемых фильмов");
        //сразу создаём корректную подстроку для поиска
        //String searchPattern = "%" + query + "%";
        String searchPattern = "%" + query.toLowerCase() + "%";
        List<Integer> filmsIds = new ArrayList<>();
        //если параметр один
        if (searchColumns.length == 1) {
            String searchWord = searchColumns[0];
            filmsIds = getIdsByOneParameter(searchPattern, searchWord);
        } else if (searchColumns.length == 2) {
            filmsIds = getIdsByTwoParameter(searchPattern, searchColumns);
        }
        if (filmsIds.isEmpty()) {
            log.info("Не точный запрос: Не найдено фильмов под требуемым параметрам");
            return List.of();
        }
        //Получили айдишники фильмов. Переходим к сортировке
        log.info("Попытка получить список всех найденных фильмов:");
        List<Film> films = getFilmsById(filmsIds);
        log.info("Результаты в методе searchedFilms(неотсортированные фильмы): {}", films);
        //Сортировка по лайкам
        log.info("Попытка получить количество лайков для фильмов");
        // Получаем количество лайков для каждого фильма
        String sqlLikesCount = "SELECT film_id, COUNT(*) AS like_count " +
                "FROM film_likes " +
                "WHERE LOWER(film_id) IN (:filmIds) " +
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

    //ищем по одной из таблиц
    private List<Integer> getIdsByOneParameter(String searchPattern, String searchColumn) {
        if (searchColumn.equals("title")) {
            String sqlQuery = "SELECT film_id FROM film WHERE LOWER(name) LIKE LOWER(:searchPattern)";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("searchPattern", searchPattern);
            List<Integer> filmsIds = jdbcOperations.query(sqlQuery, namedParameters, (rs, rowNum) -> {
                return rs.getInt("film_id");
            });
            log.info("Найденные ID фильмов: {}", filmsIds);
            return filmsIds;
        }
        if (searchColumn.equals("director")) {
            log.info("Получаем списки режиссеров, которые соответствуют подстроке {}", searchPattern);
            String sqlDirectorQuery = "SELECT director_id FROM directors WHERE name LIKE :searchPattern";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("searchPattern", searchPattern);
            List<Integer> directorIds = jdbcOperations.query(sqlDirectorQuery, namedParameters,
                    (rs, rowNum) -> rs.getInt("director_id"));
            log.info("Айди режиссеров, имя которых соответствует переданной подстроке: {}", directorIds);
            if (directorIds.isEmpty()) {
                log.info("Не найдено режиссёров по запросу: {}", searchPattern);
                return Collections.emptyList();
            }
            String sqlFilmsQuery = "SELECT film_id FROM films_directors WHERE director_id IN (:directorIds)";
            MapSqlParameterSource filmParams = new MapSqlParameterSource("directorIds", directorIds);
            List<Integer> filmsIds = jdbcOperations.query(sqlFilmsQuery, filmParams, (rs, rowNum) -> rs.getInt("film_id"));

            log.info("Найденные ID фильмов по режиссёру: {}", filmsIds);
            return filmsIds;
        }
        return Collections.emptyList();
    }

    //ищем сразу по двум таблицам, удаляем дубликаты
    private List<Integer> getIdsByTwoParameter(String searchPattern, String[] searchColumns) {
        // Инициализация переменных для хранения результатов
        List<Integer> filmsIdsByTitle = new ArrayList<>();
        List<Integer> filmsIdsByDirector = new ArrayList<>();

        // Проверка и обработка параметра "title"
        if (Arrays.asList(searchColumns).contains("title")) {
            String sqlTitleQuery = "SELECT film_id FROM film WHERE LOWER(name) LIKE LOWER(:searchPattern)";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("searchPattern", searchPattern);
            filmsIdsByTitle = jdbcOperations.query(sqlTitleQuery, namedParameters, (rs, rowNum) -> rs.getInt("film_id"));
            log.info("Найденные фильмы по названию: {}", filmsIdsByTitle);
        }
        // Проверка и обработка параметра "director"
        if (Arrays.asList(searchColumns).contains("director")) {
            log.info("Ищем фильмы по режиссёру");
            String sqlDirectorQuery = "SELECT director_id FROM directors WHERE name LIKE :searchPattern";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("searchPattern", searchPattern);
            List<Integer> directorIds = jdbcOperations.query(sqlDirectorQuery, namedParameters,
                    (rs, rowNum) -> rs.getInt("director_id"));

            if (!directorIds.isEmpty()) {
                // Ищем фильмы, связанные с найденными режиссёрами
                String sqlFilmsQuery = "SELECT film_id FROM films_directors WHERE director_id IN (:directorIds)";
                MapSqlParameterSource filmParams = new MapSqlParameterSource("directorIds", directorIds);
                filmsIdsByDirector = jdbcOperations.query(sqlFilmsQuery, filmParams, (rs, rowNum) -> rs.getInt("film_id"));
                log.info("Найденные фильмы по режиссёрам: {}", filmsIdsByDirector);
            }
        }
        // Объединяем результаты, убираем дубликаты и возвращаем
        filmsIdsByTitle.addAll(filmsIdsByDirector);
        return filmsIdsByTitle.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void deleteFilm(Long filmId) {
        String sql = "DELETE FROM film WHERE film_id = :filmId";
        if (jdbcOperations == null) {
            log.error("Ошибка: jdbcOperations = NULL! Spring не смог его создать.");
            throw new IllegalStateException("jdbcOperations не инициализирован!");
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("filmId", filmId);
        jdbcOperations.update(sql, params);
    }

    public void removeDirectorsFromFilm(Long filmId) {
        String sql = "DELETE FROM films_directors WHERE film_id = :filmId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("filmId", filmId);
        jdbcOperations.update(sql, params);
    }

    private boolean isDirectorExist(Integer directorId) {
        String sql = "SELECT COUNT(*) FROM directors WHERE director_id = :directorId";
        MapSqlParameterSource params = new MapSqlParameterSource("directorId", directorId);
        Integer count = jdbcOperations.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

}