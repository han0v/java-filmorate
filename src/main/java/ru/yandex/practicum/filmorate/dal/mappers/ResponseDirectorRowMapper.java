package ru.yandex.practicum.filmorate.dal.mappers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.mapper.ResponseDirector;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Component
public class ResponseDirectorRowMapper implements RowMapper<ResponseDirector> {
    @Override
    public ResponseDirector mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResponseDirector response = new ResponseDirector();
        response.setId(rs.getInt("director_id"));
        response.setName(rs.getString("name"));
        return response;
    }
}