package ru.yandex.practicum.filmorate.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    private Long eventId;
    private Long timestamp;
    private Long userId;
    private EventType eventType; // LIKE, REVIEW, FRIEND
    private EventOperation operation; // ADD, REMOVE, UPDATE
    private Long entityId;
}

