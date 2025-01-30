package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.ResponseDirectorRowMapper;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.exception.DirectorNotFoundException;
import ru.yandex.practicum.filmorate.exception.ErrorAddingData;
import ru.yandex.practicum.filmorate.mapper.RequestDirector;
import ru.yandex.practicum.filmorate.mapper.ResponseDirector;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;

@Slf4j
@Qualifier("directorDbStorage")
@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final NamedParameterJdbcTemplate jdbc;
    private final ResponseDirectorRowMapper responseMapper;

    @Override
    public List<DirectorDto> getAllDirectors() {
        log.info("В классе {} запущен метод по получению списка всех режиссеров", DirectorDbStorage.class.getName());
        String query = "SELECT * from directors";
        try {
            return jdbc.query(query, responseMapper).stream().map(ResponseDirector::getDirectorDto).toList();
        } catch (DataAccessException e) {
            log.debug("Ошибка при формировании списка режиссеров из БД: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseDirector getDirectorById(Integer id) {
        log.info("В классе {} запущен метод по получению режиссера с id = {}", DirectorDbStorage.class.getName(), id);
        SqlParameterSource namedParameters = new MapSqlParameterSource("id", id);
        String query = "SELECT * FROM directors WHERE director_id = :id;";
        ResponseDirector responseDirector;
        try {
            responseDirector = jdbc.queryForObject(query, namedParameters, responseMapper);
        } catch (EmptyResultDataAccessException  e) {
            log.error("Ошибка при получении режиссера из БД: {}", e.getMessage());
            throw new DirectorNotFoundException("Режиссер не найден в базе данных");
        }
        log.info("Режиссер с id = {} успешно создан в базе данных", responseDirector.getId());
        return responseDirector;
    }

    @Override
    public ResponseDirector createDirector(RequestDirector request) {
        log.info("В классе {} запущен метод по созданию режиссера {}", DirectorDbStorage.class.getName(), request);
        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("name", request.getName());
        String query = "INSERT INTO directors (name) VALUES (:name)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(query, namedParameters, keyHolder, new String[] {"director_id"});
            Integer generatedId = (Integer) keyHolder.getKey();
            return new ResponseDirector(generatedId, request.getName());
        } catch (DataAccessException e) {
            log.debug("Ошибка при создании директора из БД: {}", e.getMessage());
            throw new ErrorAddingData(e.getMessage());
        }
    }

    @Override
    public ResponseDirector updateDirector(RequestDirector request) {
        log.info("В классе {} запущен метод по обновлению режиссера {}", DirectorDbStorage.class.getName(), request);
        isDirectorExist(request.getId());
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("name", request.getName())
                .addValue("id", request.getId());
        String query = "UPDATE directors SET name = :name WHERE director_id = :id";
        int result;
        try {
            result = jdbc.update(query, namedParameters);
        } catch (DataAccessException e) {
            log.debug("Ошибка при обновлении режиссера в БД: {}", e.getMessage());
            throw new ErrorAddingData(e.getMessage());
        }
        if (result == 0) {
            log.debug("Ошибка при обновлении режиссера, данные не были изменены.");
            throw new ErrorAddingData("Данные не были обновлены");
        }
        return getDirectorById(request.getId());
    }

    @Override
    public void deleteDirector(Integer id) {
        log.info("В классе {} запущен метод по удалению режиссера с id = {}",
                DirectorDbStorage.class.getName(),
                id);
        SqlParameterSource namedParameters = new MapSqlParameterSource("id", id);
        String query = "DELETE from directors WHERE director_id = :id";
        int deleted = jdbc.update(query, namedParameters);
        if (deleted == 0) {
            log.warn("Попытка удалить несуществующего режиссера с id = {}", id);
            throw new DirectorNotFoundException("Режиссер не найден");
        }
    }

    private void isDirectorExist(Integer id) {
        log.info("В классе {} запущен метод, существует ли режиссер с id = {}",
                DirectorDbStorage.class.getName(),
                id);
        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", id);
        String query = "SELECT * FROM directors WHERE director_id = :id;";
        try {
            jdbc.queryForObject(query, namedParameters, responseMapper);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Режиссер не найден в БД: {}", e.getMessage());
            throw new DirectorNotFoundException("Режиссер не найден в базе данных");
        }
    }
}
