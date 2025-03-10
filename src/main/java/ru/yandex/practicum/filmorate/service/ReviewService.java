package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.event.Event;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.event.EventOperation;
import ru.yandex.practicum.filmorate.model.event.EventType;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final EventService eventService;

    public Review addReview(Review review) {

        reviewStorage.addReview(review);
        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(review.getUserId())
                .eventType(EventType.REVIEW)
                .operation(EventOperation.ADD)
                .entityId(review.getReviewId())
                .build();
        eventService.addEvent(event);
        return review;
    }

    public Review updateReview(Review review) {
        Review updatedReview = reviewStorage.updateReview(review);

        if (updatedReview.getReviewId() == null) {
            throw new IllegalStateException("Review ID is null after update.");
        }

        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(updatedReview.getUserId())
                .eventType(EventType.REVIEW)
                .operation(EventOperation.UPDATE)
                .entityId(updatedReview.getReviewId())
                .build();

        eventService.addEvent(event);
        return updatedReview;
    }

    public void deleteReview(Long reviewId) {

        Review review = getReviewById(reviewId);
        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(review.getUserId())
                .eventType(EventType.REVIEW)
                .operation(EventOperation.REMOVE)
                .entityId(review.getReviewId())
                .build();
        eventService.addEvent(event);
        reviewStorage.deleteReview(reviewId);
    }

    public Review getReviewById(Long reviewId) {
        return reviewStorage.getReviewById(reviewId);
    }

    public List<Review> getReviewsByFilmId(Long filmId, int count) {
        return reviewStorage.getReviewsByFilmId(filmId, count);
    }

    public void addLike(Long reviewId, Long userId) {
        reviewStorage.addLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeLike(Long reviewId, Long userId) {
        reviewStorage.removeLike(reviewId, userId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        reviewStorage.removeDislike(reviewId, userId);
    }

    public List<Review> getAllReviewsSortedByUseful() {
        return reviewStorage.getAllReviewsSortedByUseful();
    }
}