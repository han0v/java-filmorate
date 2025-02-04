package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, FilmRowMapper.class}) // Импортируем FilmDbStorage и FilmRowMapper
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;

    private Film testFilm;

    @BeforeEach
    void setUp() {
        // Создаем тестовый фильм перед каждым тестом
        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test Description");
        testFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm.setDuration(120);

        // Устанавливаем рейтинг MPA
        MpaRating mpa = new MpaRating();
        mpa.setId(1L); // Предположим, что рейтинг с ID = 1 существует в базе данных
        testFilm.setMpa(mpa);
    }

    @Test
    public void testAddFilm() {
        // Добавляем фильм
        Film addedFilm = filmStorage.addFilm(testFilm);

        // Проверяем, что фильм добавлен и его данные корректны
        assertThat(addedFilm).isNotNull();
        assertThat(addedFilm.getId()).isNotNull();
        assertThat(addedFilm.getName()).isEqualTo(testFilm.getName());
        assertThat(addedFilm.getDescription()).isEqualTo(testFilm.getDescription());
        assertThat(addedFilm.getReleaseDate()).isEqualTo(testFilm.getReleaseDate());
        assertThat(addedFilm.getDuration()).isEqualTo(testFilm.getDuration());
        assertThat(addedFilm.getMpa().getId()).isEqualTo(testFilm.getMpa().getId());
    }

    @Test
    public void testUpdateFilm() {
        // Добавляем фильм
        Film addedFilm = filmStorage.addFilm(testFilm);

        // Обновляем данные фильма
        addedFilm.setName("Updated Film");
        Film updatedFilm = filmStorage.updateFilm(addedFilm);

        // Проверяем, что данные обновлены
        assertThat(updatedFilm).isNotNull();
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
    }

    @Test
    public void testGetFilmById() {
        // Добавляем фильм
        Film addedFilm = filmStorage.addFilm(testFilm);

        // Получаем фильм по ID
        Film foundFilm = filmStorage.getFilmById(addedFilm.getId());

        // Проверяем, что фильм найден и его данные корректны
        assertThat(foundFilm).isNotNull();
        assertThat(foundFilm.getId()).isEqualTo(addedFilm.getId());
        assertThat(foundFilm.getName()).isEqualTo(addedFilm.getName());
    }

    @Test
    public void testGetAllFilms() {
        // Добавляем фильм
        filmStorage.addFilm(testFilm);

        // Получаем все фильмы
        Collection<Film> films = filmStorage.getAllFilms();

        // Проверяем, что список не пустой и содержит добавленный фильм
        assertThat(films).isNotEmpty();
        assertThat(films).hasSize(1);
    }
}