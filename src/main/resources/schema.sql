-- Таблица рейтингов (MPA_RATING)
CREATE TABLE IF NOT EXISTS MPA_RATING (
   rating_id INTEGER PRIMARY KEY,
   rating_MPA VARCHAR(50) NOT NULL
);

-- Таблица жанров (GENRES)
CREATE TABLE IF NOT EXISTS GENRES (
    genre_id INTEGER PRIMARY KEY,
    genre VARCHAR(50) NOT NULL
);

-- Таблица пользователей (users)
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    login VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    birthday DATE
);

-- Таблица фильмов (film)
CREATE TABLE IF NOT EXISTS film (
    film_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    release_date DATE,
    duration INTEGER,
    rating_id INTEGER,
    FOREIGN KEY (rating_id) REFERENCES MPA_RATING(rating_id)
);

-- Промежуточная таблица для связи фильмов и жанров
CREATE TABLE IF NOT EXISTS film_genre (
    film_id INTEGER,
    genre_id INTEGER,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES film(film_id),
    FOREIGN KEY (genre_id) REFERENCES GENRES(genre_id)
);

CREATE TABLE IF NOT EXISTS FRIENDS (
    user_id INT,
    friend_id INT,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (friend_id) REFERENCES users(user_id)
);

-- Таблица лайков (film_likes)
CREATE TABLE IF NOT EXISTS film_likes (
    film_id INTEGER,
    user_id INTEGER,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

