package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>();
    private long currentId = 1;

    @Override
    public Film addFilm(Film film) {
        film.setId(currentId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film updateFilm(Film newFilm) {
        if (newFilm.getId() == null || !films.containsKey(newFilm.getId())) {
            throw new IllegalArgumentException("Фильм с id = " + newFilm.getId() + " не найден");
        }
        films.put(newFilm.getId(), newFilm);
        return newFilm;
    }

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        Set<Long> filmLikes = likes.computeIfAbsent(filmId, k -> new HashSet<>());
        if (filmLikes.contains(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }
        filmLikes.add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Set<Long> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public Film getFilmById(Long filmId) {
        return films.get(filmId);
    }

    @Override
    public List<Film> getTop10Films(int count) {
        List<Film> sortedFilms = new ArrayList<>(getAllFilms());

        // Сортировка фильмов по количеству лайков в порядке убывания
        sortedFilms.sort((film1, film2) -> {
            int likes1 = likes.getOrDefault(film1.getId(), Collections.emptySet()).size();
            int likes2 = likes.getOrDefault(film2.getId(), Collections.emptySet()).size();
            return Integer.compare(likes2, likes1); // Сортировка по убыванию
        });

        // Возвращаем топ 10 фильмов или меньше, если их меньше 10
        return sortedFilms.stream().limit(count).collect(Collectors.toList());
    }

}
