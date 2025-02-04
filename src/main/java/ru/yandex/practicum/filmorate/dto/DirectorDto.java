package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DirectorDto {
    private Integer id;
    private String name;
}
