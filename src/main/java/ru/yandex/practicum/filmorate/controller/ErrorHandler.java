package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.DirectorNotFoundException;

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
}
