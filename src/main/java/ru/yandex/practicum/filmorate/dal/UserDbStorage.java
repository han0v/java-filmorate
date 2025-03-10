package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Qualifier("userDbStorage")
@Primary
public class UserDbStorage implements UserStorage {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final UserRowMapper userRowMapper; // Добавляем UserRowMapper
    private final FilmRowMapper filmRowMapper;
    private final GenreDbStorage genreDbStorage;
    private final DirectorDbStorage directorDbStorage;

    @Autowired
    public UserDbStorage(NamedParameterJdbcOperations jdbcOperations, UserRowMapper userRowMapper,
                         FilmRowMapper filmRowMapper, GenreDbStorage genreDbStorage,
                         DirectorDbStorage directorDbStorage) {
        this.jdbcOperations = jdbcOperations;
        this.userRowMapper = userRowMapper; // Инициализация UserRowMapper
        this.filmRowMapper = filmRowMapper;
        this.genreDbStorage = genreDbStorage;
        this.directorDbStorage = directorDbStorage;
    }

    @Override
    public User addUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        String sql = "INSERT INTO users (login, name, email, birthday) VALUES (:login, :name, :email, :birthday)";
        Map<String, Object> params = new HashMap<>();
        params.put("login", user.getLogin());
        params.put("email", user.getEmail());
        params.put("name", user.getName());
        params.put("birthday", user.getBirthday());

        try {
            jdbcOperations.update(sql, params);
            Long userId = jdbcOperations.queryForObject("SELECT MAX(user_id) FROM users", params, Long.class);
            user.setId(userId);

            return user;
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
            throw new RuntimeException("Произошла непредвиденная ошибка при добавлении пользователя", e);
        }
    }


    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = :email, login = :login, name = :name, birthday = :birthday WHERE user_id = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("email", user.getEmail());
        params.put("login", user.getLogin());
        params.put("name", user.getName());
        params.put("birthday", user.getBirthday());
        params.put("userId", user.getId());

        jdbcOperations.update(sql, params);
        return user;
    }

    @Override
    public Collection<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcOperations.query(sql, userRowMapper);
    }

    @Override
    public User getUserById(Long userId) {
        log.info("Запрос на получение пользователя с id = {}", userId);
        String sql = "SELECT * FROM users WHERE user_id = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        try {
            User user = jdbcOperations.queryForObject(sql, params, userRowMapper);
            log.info("Найден пользователь: {}", user);
            return user;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с id = {} не найден", userId);
            return null; // Вернуть null если пользователь не найден
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя с id = {}: {}", userId, e.getMessage());
            throw e; // Пробросить исключение
        }
    }


    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO FRIENDS (user_id, friend_id) VALUES (:userId, :friendId)";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("friendId", friendId);

        jdbcOperations.update(sql, params);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM FRIENDS WHERE user_id = :userId AND friend_id = :friendId";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("friendId", friendId);

        jdbcOperations.update(sql, params);
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        String sql = "SELECT u.* FROM FRIENDS f1 " +
                "JOIN FRIENDS f2 ON f1.friend_id = f2.friend_id " +
                "JOIN users u ON f1.friend_id = u.user_id " +
                "WHERE f1.user_id = :userId1 AND f2.user_id = :userId2";
        Map<String, Object> params = new HashMap<>();
        params.put("userId1", userId1);
        params.put("userId2", userId2);

        return jdbcOperations.query(sql, params, userRowMapper); // Используем UserRowMapper
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM FRIENDS f " +
                "JOIN users u ON f.friend_id = u.user_id " +
                "WHERE f.user_id = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        return jdbcOperations.query(sql, params, userRowMapper); // Используем UserRowMapper
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        String sql = "SELECT DISTINCT f.*, mr.rating_mpa FROM film f " +
                "JOIN film_likes fl_similar ON f.film_id = fl_similar.film_id " +
                "JOIN mpa_rating mr ON f.rating_id = mr.rating_id " +
                "WHERE fl_similar.user_id IN (" +
                "    SELECT fl_other.user_id " +
                "    FROM film_likes fl_user " +
                "    JOIN film_likes fl_other ON fl_user.film_id = fl_other.film_id " +
                "    WHERE fl_user.user_id = :userId " +
                "      AND fl_other.user_id != :userId " +
                "    GROUP BY fl_other.user_id " +
                "    ORDER BY COUNT(fl_other.film_id) DESC " +
                "    LIMIT 1 " +
                ") " +
                "AND f.film_id NOT IN (" +
                "    SELECT film_id FROM film_likes WHERE user_id = :userId " +
                ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId);
        try {
            List<Film> films = jdbcOperations.query(sql, params, filmRowMapper);

            for (Film film : films) {
                film.setGenres(genreDbStorage.getGenresForFilm(film.getId()));
                film.setDirectors(directorDbStorage.getDirectorsForFilm(film.getId()));
            }
            return films;
        } catch (Exception e) {
            log.error("Ошибка при выполнении SQL-запроса: ", e);
            throw e;
        }
    }

    @Override
    public void deleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = :userId";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        jdbcOperations.update(sql, params);
    }
}
