package ru.yandex.practicum.filmorate.exception;

public class ErrorAddingData extends RuntimeException {
    public ErrorAddingData(String message) {
        super(message);
    }
}
