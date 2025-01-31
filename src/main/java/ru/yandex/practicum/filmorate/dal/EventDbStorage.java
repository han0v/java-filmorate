package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.EventRowMapper;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Repository
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;
    private final EventRowMapper eventRowMapper;

    public EventDbStorage(JdbcTemplate jdbcTemplate, EventRowMapper eventRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventRowMapper = eventRowMapper;
    }

    @Override
    public void addEvent(Event event) {
        String sql = "INSERT INTO events (timestamp, user_id, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, event.getTimestamp());
            ps.setLong(2, event.getUserId());
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getOperation());
            ps.setLong(5, event.getEntityId());
            return ps;
        }, keyHolder);

        // Устанавливаем сгенерированный event_id в объект Event
        Long generatedId = keyHolder.getKey().longValue();
        event.setEventId(generatedId); // Обновляем объект Event
    }

    @Override
    public List<Event> getUserFeed(Long userId) {
        String sql = "SELECT * FROM events WHERE user_id = ?";
        return jdbcTemplate.query(sql, eventRowMapper, userId);
    }
}
