package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
    @Qualifier("userDbStorage")
    private final UserStorage userStorage;
    private final EventService eventService;

    public User addUser(User user) {
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        return userStorage.updateUser(user);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public void addFriend(Long userId, Long friendId) {
        userStorage.addFriend(userId, friendId);
        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType("FRIEND")
                .operation("ADD")
                .entityId(friendId)
                .build();
        log.info("Event add entityId= " + userId);
        eventService.addEvent(event);
    }

    public void removeFriend(Long userId, Long friendId) {
        userStorage.removeFriend(userId, friendId);

        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType("FRIEND")
                .operation("REMOVE")
                .entityId(friendId)
                .build();

        eventService.addEvent(event);
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        return userStorage.getCommonFriends(userId1, userId2);
    }

    public User getUserById(Long userId) {
        return userStorage.getUserById(userId);
    }

    public List<User> getFriends(Long userId) {
        return userStorage.getFriends(userId);
    }

    public List<Film> getRecommendations(Long userId) {
        return userStorage.getRecommendations(userId);
    }

    public void deleteUser(Long userId) {
        userStorage.deleteUser(userId);
    }
}
