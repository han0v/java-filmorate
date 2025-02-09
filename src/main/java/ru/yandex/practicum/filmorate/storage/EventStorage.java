package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.event.Event;

import java.util.List;

public interface EventStorage {

    void addEvent(Event event);

    List<Event> getUserFeed(Long userId);
}
