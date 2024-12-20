package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();
    private long currentId = 1;

    @Override
    public User addUser(User user) {
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }
        user.setId(currentId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User newUser) {
        if (newUser.getId() == null || !users.containsKey(newUser.getId())) {
            throw new IllegalArgumentException("Пользователь с id = " + newUser.getId() + " не найден");
        }
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    @Override
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @Override
    public User getUserById(Long userId) {
        return users.get(userId);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new ValidationException("Один или оба пользователя не найдены");
        }
        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        friends.computeIfAbsent(friendId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        Set<Long> userFriends = friends.get(userId);
        Set<Long> friendFriends = friends.get(friendId);
        if (userFriends != null) {
            userFriends.remove(friendId);
        }
        if (friendFriends != null) {
            friendFriends.remove(userId);
        }
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        Set<Long> friendsUser1 = friends.getOrDefault(userId1, Collections.emptySet());
        Set<Long> friendsUser2 = friends.getOrDefault(userId2, Collections.emptySet());

        Set<Long> commonFriendIds = new HashSet<>(friendsUser1);
        commonFriendIds.retainAll(friendsUser2);

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : commonFriendIds) {
            User friendUser = users.get(friendId);
            if (friendUser != null) {
                commonFriends.add(friendUser);
            }
        }
        return commonFriends;
    }


    @Override
    public List<User> getFriends(Long userId) {
        if (userId == null || !users.containsKey(userId)) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден");
        }
        Set<Long> friendIds = friends.getOrDefault(userId, Collections.emptySet());

        // Получаем полные объекты User из идентификаторов друзей
        List<User> friendUsers = new ArrayList<>();
        for (Long friendId : friendIds) {
            User friendUser = users.get(friendId);
            if (friendUser != null) {
                friendUsers.add(friendUser);
            }
        }
        return friendUsers;
    }

}
