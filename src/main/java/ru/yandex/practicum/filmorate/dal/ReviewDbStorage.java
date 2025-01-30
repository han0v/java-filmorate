package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dal.mappers.ReviewRowMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ReviewDbStorage implements ReviewStorage {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final ReviewRowMapper reviewRowMapper;

    @Autowired
    public ReviewDbStorage(NamedParameterJdbcOperations jdbcOperations, ReviewRowMapper reviewRowMapper) {
        this.jdbcOperations = jdbcOperations;
        this.reviewRowMapper = reviewRowMapper;
    }

    @Override
    public Review addReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) " +
                "VALUES (:content, :isPositive, :userId, :filmId, :useful)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Map<String, Object> params = new HashMap<>();
        params.put("content", review.getContent());
        params.put("isPositive", review.getIsPositive());
        params.put("userId", review.getUserId());
        params.put("filmId", review.getFilmId());
        params.put("useful", 0);

        jdbcOperations.update(sql, new MapSqlParameterSource(params), keyHolder);
        review.setReviewId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        return review;
    }


    @Override
    public Review updateReview(Review review) {
        String sql = "UPDATE reviews SET content = :content, is_positive = :isPositive, useful = :useful " +
                "WHERE review_id = :reviewId";

        Map<String, Object> params = new HashMap<>();
        params.put("content", review.getContent());
        params.put("isPositive", review.getIsPositive());
        params.put("useful", review.getUseful());
        params.put("reviewId", review.getReviewId());

        jdbcOperations.update(sql, params);
        if (review.getReviewId() == null) {
            throw new IllegalArgumentException("Отзыв с id = " + review.getReviewId() + " не найден.");
        }
        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReview(Long reviewId) {
        String sql = "DELETE FROM reviews WHERE review_id = :reviewId";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);

        jdbcOperations.update(sql, params);
    }


    @Override
    public Review getReviewById(Long reviewId) {
        log.info("Запрос на получение отзыва с id = {}", reviewId);
        String sql = "SELECT * FROM reviews WHERE review_id = :reviewId";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);

        try {
            Review review = jdbcOperations.queryForObject(sql, params, reviewRowMapper);
            log.info("Найден отзыв: {}", review);
            return review;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с id = {} не найден", reviewId);
            return null;
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя с id = {}: {}", reviewId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Review> getReviewsByFilmId(Long filmId, int count) {
        String sql = "SELECT * FROM reviews WHERE film_id = :filmId ORDER BY useful DESC LIMIT :count";
        Map<String, Object> params = new HashMap<>();
        params.put("filmId", filmId);
        params.put("count", count);

        return jdbcOperations.query(sql, params, reviewRowMapper);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (:reviewId, :userId, true)";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);
        params.put("userId", userId);

        jdbcOperations.update(sql, params);
        updateUseful(reviewId, 1);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        // Проверка существования лайка
        String checkSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = :reviewId AND user_id = :userId AND is_like = true";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);
        params.put("userId", userId);

        Integer count = jdbcOperations.queryForObject(checkSql, params, Integer.class);

        if (count != null && count > 0) {
            // Если лайк существует, удаляем его
            removeLike(reviewId, userId);
        }

        // Проверка существования дизлайка
        String checkDislikeSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = :reviewId AND user_id = :userId AND is_like = false";
        Integer dislikeCount = jdbcOperations.queryForObject(checkDislikeSql, params, Integer.class);

        if (dislikeCount == null || dislikeCount == 0) {
            String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (:reviewId, :userId, false)";
            jdbcOperations.update(sql, params);
            updateUseful(reviewId, -1);
        } else {
            log.warn("Пользователь с id = {} уже поставил дизлайк на отзыв id = {}", userId, reviewId);
        }
    }


    @Override
    public void removeLike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = :reviewId AND user_id = :userId AND is_like = true";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);
        params.put("userId", userId);

        jdbcOperations.update(sql, params);
        updateUseful(reviewId, -1);
    }

    @Override
    public void removeDislike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = :reviewId AND user_id = :userId AND is_like = false";
        Map<String, Object> params = new HashMap<>();
        params.put("reviewId", reviewId);
        params.put("userId", userId);

        jdbcOperations.update(sql, params);
        updateUseful(reviewId, 1);
    }

    private void updateUseful(Long reviewId, int delta) {
        String sql = "UPDATE reviews SET useful = useful + :delta WHERE review_id = :reviewId";
        Map<String, Object> params = new HashMap<>();
        params.put("delta", delta);
        params.put("reviewId", reviewId);

        jdbcOperations.update(sql, params);
    }
}