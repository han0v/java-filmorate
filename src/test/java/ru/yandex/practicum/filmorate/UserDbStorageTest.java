package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class, UserRowMapper.class})
class UserDbStorageTest {
    private final UserDbStorage userStorage;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя перед каждым тестом
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setLogin("testLogin");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    public void testFindUserById() {
        User addedUser = userStorage.addUser(testUser);
        Optional<User> userOptional = Optional.ofNullable(userStorage.getUserById(addedUser.getId()));
        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user).hasFieldOrPropertyWithValue("id", addedUser.getId())
                );
    }

    @Test
    public void testAddUser() {
        // Добавляем пользователя
        User addedUser = userStorage.addUser(testUser);

        // Проверяем, что пользователь добавлен и его данные корректны
        assertThat(addedUser).isNotNull();
        assertThat(addedUser.getId()).isNotNull();
        assertThat(addedUser.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(addedUser.getLogin()).isEqualTo(testUser.getLogin());
        assertThat(addedUser.getName()).isEqualTo(testUser.getName());
        assertThat(addedUser.getBirthday()).isEqualTo(testUser.getBirthday());
    }

    @Test
    public void testUpdateUser() {
        // Добавляем пользователя
        User addedUser = userStorage.addUser(testUser);

        // Обновляем данные пользователя
        addedUser.setName("Updated Name");
        User updatedUser = userStorage.updateUser(addedUser);

        // Проверяем, что данные обновлены
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    public void testGetAllUsers() {
        // Добавляем пользователя
        userStorage.addUser(testUser);

        // Получаем всех пользователей
        Collection<User> users = userStorage.getAllUsers();

        // Проверяем, что список не пустой и содержит добавленного пользователя
        assertThat(users).isNotEmpty();
        assertThat(users).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void testAddFriend() {
        // Добавляем двух пользователей
        User user1 = userStorage.addUser(testUser);
        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friendLogin");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.addUser(user2);

        // Добавляем друга
        userStorage.addFriend(user1.getId(), user2.getId());

        // Получаем список друзей
        List<User> friends = userStorage.getFriends(user1.getId());

        // Проверяем, что друг добавлен
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(user2.getId());
    }

    @Test
    public void testRemoveFriend() {
        // Добавляем двух пользователей
        User user1 = userStorage.addUser(testUser);
        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friendLogin");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.addUser(user2);

        // Добавляем друга
        userStorage.addFriend(user1.getId(), user2.getId());

        // Удаляем друга
        userStorage.removeFriend(user1.getId(), user2.getId());

        // Получаем список друзей
        List<User> friends = userStorage.getFriends(user1.getId());

        // Проверяем, что друг удален
        assertThat(friends).isEmpty();
    }

    @Test
    public void testGetCommonFriends() {
        // Добавляем трех пользователей
        User user1 = userStorage.addUser(testUser);
        User user2 = new User();
        user2.setEmail("friend1@example.com");
        user2.setLogin("friend1Login");
        user2.setName("Friend 1");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.addUser(user2);

        User user3 = new User();
        user3.setEmail("friend2@example.com");
        user3.setLogin("friend2Login");
        user3.setName("Friend 2");
        user3.setBirthday(LocalDate.of(1996, 6, 6));
        user3 = userStorage.addUser(user3);

        // Добавляем общих друзей
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.addFriend(user3.getId(), user2.getId());

        // Получаем общих друзей
        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user3.getId());

        // Проверяем, что общий друг найден
        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.get(0).getId()).isEqualTo(user2.getId());
    }
}