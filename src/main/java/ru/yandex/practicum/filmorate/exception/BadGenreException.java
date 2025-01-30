package ru.yandex.practicum.filmorate.exception;

public class BadGenreException extends RuntimeException {
    public BadGenreException(String message) {
        super(message);
    }
}
