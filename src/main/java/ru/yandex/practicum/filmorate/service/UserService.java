package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {
    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

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
    }

    public void removeFriend(Long userId, Long friendId) {
        userStorage.removeFriend(userId, friendId);
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
}
