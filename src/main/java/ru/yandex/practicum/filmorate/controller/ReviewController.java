package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.ReviewDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final UserService userService;
    private final FilmService filmService;

    @PostMapping
    public ResponseEntity<ReviewDto> addReview(@RequestBody ReviewDto reviewDto) {
        Review review = ReviewMapper.toReview(reviewDto);
        validateReview(review);
        validateIds(review.getUserId(), review.getFilmId());
        Review createdReview = reviewService.addReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewMapper.toReviewDto(createdReview));
    }

    @PutMapping
    public ResponseEntity<ReviewDto> updateReview(@RequestBody ReviewDto reviewDto) {
        Review review = ReviewMapper.toReview(reviewDto);
        validateReview(review); // Валидация отзыва
        validateIds(review.getUserId(), review.getFilmId());
        Review updatedReview = reviewService.updateReview(review);
        return ResponseEntity.ok(ReviewMapper.toReviewDto(updatedReview));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDto> getReviewById(@PathVariable Long id) {
        log.info("Запрос на получение отзыва с id = {}", id);
        Review review = reviewService.getReviewById(id);
        if (review == null) {
            throw new NotFoundException("Отзыв с id = " + id + " не найден.");
        }
        return ResponseEntity.ok(ReviewMapper.toReviewDto(review));
    }


    @GetMapping
    public ResponseEntity<List<ReviewDto>> getReviews(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") int count) {

        List<Review> reviews;

        if (filmId != null) {
            reviews = reviewService.getReviewsByFilmId(filmId, count); // передаём count
        } else {
            reviews = reviewService.getAllReviewsSortedByUseful(); // без count
        }

        List<ReviewDto> reviewDtos = reviews.stream()
                .map(ReviewMapper::toReviewDto)
                .toList();
        return ResponseEntity.ok(reviewDtos);
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(
            @PathVariable Long id,
            @PathVariable Long userId) {
        validateIds(userId, null); // Проверка существования userId
        reviewService.addLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> addDislike(
            @PathVariable Long id,
            @PathVariable Long userId) {
        validateIds(userId, null); // Проверка существования userId
        reviewService.addDislike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(
            @PathVariable Long id,
            @PathVariable Long userId) {
        validateIds(userId, null); // Проверка существования userId
        reviewService.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> removeDislike(
            @PathVariable Long id,
            @PathVariable Long userId) {
        validateIds(userId, null); // Проверка существования userId
        reviewService.removeDislike(id, userId);
        return ResponseEntity.ok().build();
    }

    private void validateReview(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            log.error("Ошибка валидации: Содержание отзыва не может быть пустым.");
            throw new ValidationException("Содержание отзыва не может быть пустым.");
        }
        if (review.getIsPositive() == null) {
            log.error("Ошибка валидации: Тип отзыва (положительный/отрицательный) должен быть указан.");
            throw new ValidationException("Тип отзыва (положительный/отрицательный) должен быть указан.");
        }
        if (review.getUserId() == null) {
            log.error("Ошибка валидации: Идентификатор пользователя должен быть указан.");
            throw new ValidationException("Идентификатор пользователя должен быть указан.");
        }
        if (review.getUserId() <= 0) {
            log.error("Ошибка валидации: Идентификатор пользователя должен быть положительным числом.");
            throw new NotFoundException("Идентификатор пользователя должен быть положительным числом.");
        }
        if (review.getFilmId() == null) {
            log.error("Ошибка валидации: Идентификатор фильма должен быть указан.");
            throw new ValidationException("Ошибка валидации: Идентификатор фильма должен быть указан.");
        }
        if (review.getFilmId() <= 0) {
            log.error("Ошибка валидации: Идентификатор фильма должен быть положительным числом.");
            throw new NotFoundException("Идентификатор фильма должен быть положительным числом.");
        }
    }

    private void validateIds(Long userId, Long filmId) {
        if (userId != null && userService.getUserById(userId) == null) {
            log.error("Ошибка валидации: Пользователь с id = {} не найден.", userId);
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
        if (filmId != null && filmService.getFilmById(filmId) == null) {
            log.error("Ошибка валидации: Фильм с id = {} не найден.", filmId);
            throw new NotFoundException("Фильм с id = " + filmId + " не найден.");
        }
    }
}