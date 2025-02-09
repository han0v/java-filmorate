package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.BadGenreException;
import ru.yandex.practicum.filmorate.exception.BadMpaException;
import ru.yandex.practicum.filmorate.exception.DirectorNotFoundException;
import ru.yandex.practicum.filmorate.exception.ErrorAddingData;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> notDirectorFound(final DirectorNotFoundException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "режиссер не найден в БД");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> badMpa(final BadMpaException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Не валидный MPA");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> badGenre(final BadGenreException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Несуществующий жанр");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> errorAddingData(final ErrorAddingData e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Ошибка добавления данных");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> illegalArgument(final IllegalArgumentException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Переданы неверные данные");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> notFound(final NotFoundException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Данные не найдены");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> badValidation(final ValidationException e) {
        log.error("Перехвачено исключение {}", e.toString());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Ошибка валидации данных");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
