-- =============================================================================
-- Database Schema for AI-Based Movie Recommendation System (MySQL 8.0+)
-- Database: movie_recommendation_db
-- =============================================================================

CREATE DATABASE IF NOT EXISTS movie_recommendation_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE movie_recommendation_db;

-- -----------------------------------------------------------------------------
-- Table: movies
-- Stores movie metadata retrieved from TMDB API with duplicate prevention (tmdb_id UNIQUE)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movies (
    movie_id INT AUTO_INCREMENT PRIMARY KEY,
    tmdb_id INT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    release_year INT,
    overview TEXT,
    genres VARCHAR(255),
    poster_path VARCHAR(255),
    rating DECIMAL(3, 1) DEFAULT 0.0,
    vote_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_movies_tmdb_id (tmdb_id),
    INDEX idx_movies_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: search_history
-- Records every movie title searched by users with timestamps
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_history (
    search_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_title VARCHAR(255) NOT NULL,
    searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_search_history_time (searched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table: recommendation_history
-- Stores generated recommendations linked to search history and cached movies
-- Demonstrates Foreign Keys, Cascade Delete, and Decimal Precision
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recommendation_history (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    search_id BIGINT NOT NULL,
    recommended_movie_id INT NOT NULL,
    similarity_score DECIMAL(5, 4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rec_search
        FOREIGN KEY (search_id) REFERENCES search_history(search_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rec_movie
        FOREIGN KEY (recommended_movie_id) REFERENCES movies(movie_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_rec_search_id (search_id),
    INDEX idx_rec_movie_id (recommended_movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
