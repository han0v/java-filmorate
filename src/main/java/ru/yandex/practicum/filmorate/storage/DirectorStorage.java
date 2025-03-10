package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.mapper.RequestDirector;
import ru.yandex.practicum.filmorate.mapper.ResponseDirector;

import java.util.List;

public interface DirectorStorage {

    List<DirectorDto> getAllDirectors();

    ResponseDirector getDirectorById(Integer id);

    ResponseDirector createDirector(RequestDirector request);

    ResponseDirector updateDirector(RequestDirector request);

    void deleteDirector(Integer id);

}
