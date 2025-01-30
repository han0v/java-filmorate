package ru.yandex.practicum.filmorate.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.dto.DirectorDto;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDirector {
    private Integer id;
    private String name;

    public DirectorDto getDirectorDto() {
        return DirectorDto.builder().id(id).name(name).build();
    }
}
