package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FilmorateApplicationTests {

	private FilmController filmController;
	private UserController userController;

	@BeforeEach
	void setUp() {
		filmController = new FilmController();
		userController = new UserController();
	}

	@Test
	void validateFilmWithEmptyNameShouldThrowException() {
		Film film = new Film();
		film.setName(""); // пустое имя
		film.setDescription("Some description");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void validateFilmWithTooLongDescriptionShouldThrowException() {
		Film film = new Film();
		film.setName("Test Film");
		film.setDescription("A".repeat(201));
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void validateFilmWithPastReleaseDateShouldThrowException() {
		Film film = new Film();
		film.setName("Test Film");
		film.setDescription("Some description");
		film.setReleaseDate(LocalDate.of(1890, 1, 1)); // до 28 декабря 1895 года
		film.setDuration(120);

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void validateFilmWithNegativeDurationShouldThrowException() {
		Film film = new Film();
		film.setName("Test Film");
		film.setDescription("Some description");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(-10); // отрицательная продолжительность

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void validateUserWithInvalidEmailShouldThrowException() {
		User user = new User();
		user.setEmail("invalidEmail");
		user.setLogin("login");
		user.setName("name");
		user.setBirthday(LocalDate.of(2000, 1, 1));

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void validateUserWithBlankLoginShouldThrowException() {
		User user = new User();
		user.setEmail("user@example.com");
		user.setLogin("");
		user.setName("name");
		user.setBirthday(LocalDate.of(2000, 1, 1));

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void validateUserWithFutureBirthdayShouldThrowException() {
		User user = new User();
		user.setEmail("user@example.com");
		user.setLogin("login");
		user.setName("name");
		user.setBirthday(LocalDate.now().plusDays(1)); // дата рождения в будущем

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void validateUserWithLoginContainingSpacesShouldThrowException() {
		User user = new User();
		user.setEmail("user@example.com");
		user.setLogin("login with spaces");
		user.setName("name");
		user.setBirthday(LocalDate.of(2000, 1, 1));

		assertThrows(ValidationException.class, () -> userController.create(user));
	}
}

