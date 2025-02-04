package ru.yandex.practicum.filmorate.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.util.List;

@Service
public class EventService {

    private final EventStorage eventStorage;
    private final UserService userService;

    public EventService(EventStorage eventStorage,@Lazy UserService userService) {
        this.eventStorage = eventStorage;
        this.userService = userService;
    }


    public void addEvent(Event event) {
        eventStorage.addEvent(event);
    }

    public List<Event> getUserFeed(Long userId) {
        if (userService.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id - " + userId + " не найден");
        }
        return eventStorage.getUserFeed(userId);
    }
}
