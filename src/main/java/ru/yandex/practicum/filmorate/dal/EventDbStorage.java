package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Repository
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    public EventDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addEvent(Event event) {
        String sql = "INSERT INTO events (timestamp, user_id, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                event.getTimestamp(),
                event.getUserId(),
                event.getEventType(),
                event.getOperation(),
                event.getEntityId());
        log.info("Событие {} добавленно", event);
    }

    @Override
    public List<Event> getUserFeed(Long userId) {
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        return new Event(
                rs.getLong("event_id"),
                rs.getLong("timestamp"),
                rs.getLong("user_id"),
                rs.getString("event_type"),
                rs.getString("operation"),
                rs.getLong("entity_id")
        );
    }
}
