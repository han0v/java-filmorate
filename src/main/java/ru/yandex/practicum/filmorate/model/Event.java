package ru.yandex.practicum.filmorate.model;

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
    private String eventType; // LIKE, REVIEW, FRIEND
    private String operation; // ADD, REMOVE, UPDATE
    private Long entityId;
}