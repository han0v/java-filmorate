package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.model.RequestDirector;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<DirectorDto> getAllDirectors() {
        return directorService.getAllDirectors();
    }

    @GetMapping("/{id}")
    public Optional<DirectorDto> getDirectorById(@PathVariable Integer id) {
        return directorService.getDirectorById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<DirectorDto> createDirector(@Valid @RequestBody RequestDirector request) {
        return directorService.createDirector(request);
    }

    @PutMapping
    public Optional<DirectorDto> updateDirector(@Valid @RequestBody RequestDirector request) {
        return directorService.updateDirector(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteDirector(@PathVariable Integer id) {
        directorService.deleteDirector(id);
    }
}
