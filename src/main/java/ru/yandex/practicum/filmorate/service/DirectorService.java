package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.RequestDirector;
import ru.yandex.practicum.filmorate.mapper.ResponseDirector;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {
    @Qualifier("directorDbStorage")
    private final DirectorStorage directorStorage;

    public List<DirectorDto> getAllDirectors() {
        log.info("В классе {} запущен метод по получению всех режиссеров", DirectorService.class.getName());
        return directorStorage.getAllDirectors();
    }

    public Optional<DirectorDto> getDirectorById(Integer id) {
        log.info("В классе {} запущен метод по получению режиссера с id = {}", DirectorService.class.getName(), id);
        return Optional.ofNullable(directorStorage.getDirectorById(id).getDirectorDto());
    }

    public Optional<DirectorDto> createDirector(RequestDirector request) {
        log.info("В классе {} запущен метод по созданию режиссера", DirectorService.class.getName());
        ResponseDirector responseDirector = directorStorage.createDirector(request);
        log.info("Создание режиссера прошло успешно {}", responseDirector);
        return Optional.ofNullable(responseDirector.getDirectorDto());
    }

    public Optional<DirectorDto> updateDirector(RequestDirector request) {
        log.info("В классе {} запущен метод по обновлению режиссера c id = {}",
                DirectorService.class.getName(),
                request.getId());
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Не задано имя режиссера");
        }
        log.info("Валидация имени режиссера прошла успешно");
        ResponseDirector responseDirector = directorStorage.updateDirector(request);
        log.info("Обновление режиссера прошло успешно");
        return Optional.ofNullable(responseDirector.getDirectorDto());
    }

    public void deleteDirector(Integer id) {
        log.info("В классе {} запущен метод по удалению режиссера с id = {}",
                DirectorService.class.getName(),
                id);
        directorStorage.deleteDirector(id);
    }
}
