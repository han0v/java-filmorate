MERGE INTO MPA_RATING (rating_id, rating_MPA)
VALUES (1, 'G');
MERGE INTO MPA_RATING (rating_id, rating_MPA)
VALUES (2, 'PG');
MERGE INTO MPA_RATING (rating_id, rating_MPA)
VALUES (3, 'PG-13');
MERGE INTO MPA_RATING (rating_id, rating_MPA)
VALUES (4, 'R');
MERGE INTO MPA_RATING (rating_id, rating_MPA)
VALUES (5, 'NC-17');

MERGE INTO GENRES (genre_id, genre)
VALUES (1, 'Комедия');
MERGE INTO GENRES (genre_id, genre)
VALUES (2, 'Драма');
MERGE INTO GENRES (genre_id, genre)
VALUES (3, 'Мультфильм');
MERGE INTO GENRES (genre_id, genre)
VALUES (4, 'Триллер');
MERGE INTO GENRES (genre_id, genre)
VALUES (5, 'Документальный');
MERGE INTO GENRES (genre_id, genre)
VALUES (6, 'Боевик');
