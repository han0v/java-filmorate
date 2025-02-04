package ru.yandex.practicum.filmorate.mapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.DirectorDto;

@Slf4j
@Data
@Component //возможно не нужен
@RequiredArgsConstructor
public class RequestDirector {
    private Integer id;
    private String name;

    public DirectorDto getDirectorDto() {
        log.info("В классе {} вызван метод по получению представления FilmDto", RequestDirector.class.getName());
        return DirectorDto.builder()
                .id(id)
                .name(name)
                .build();
    }
}
