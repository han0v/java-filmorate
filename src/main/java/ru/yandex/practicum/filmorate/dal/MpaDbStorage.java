package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MpaRating> getAllMpaRatings() {
        String sql = "SELECT * FROM mpa_rating";
        return jdbcTemplate.query(sql, this::mapRowToMpaRating);
    }

    @Override
    public MpaRating getMpaRatingById(int id) {
        String sql = "SELECT * FROM mpa_rating WHERE rating_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToMpaRating, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private MpaRating mapRowToMpaRating(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpaRating = new MpaRating();
        mpaRating.setId((long) rs.getInt("rating_id"));
        mpaRating.setName(rs.getString("rating_mpa"));
        return mpaRating;
    }
}