package com.recommendation.repository;

import com.recommendation.client.TmdbApiClient;
import com.recommendation.model.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Automatically initializes database schema tables on application startup using JDBC DDL
 * and seeds initial movie metadata to ensure immediate data availability.
 * Demonstrates JDBC DDL statement execution, schema migration, and exception handling.
 */
@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final MovieRepository movieRepository;
    private final TmdbApiClient tmdbApiClient;

    public DatabaseInitializer(DataSource dataSource, MovieRepository movieRepository, TmdbApiClient tmdbApiClient) {
        this.dataSource = dataSource;
        this.movieRepository = movieRepository;
        this.tmdbApiClient = tmdbApiClient;
    }

    @PostConstruct
    public void initializeDatabase() {
        String createMoviesTable = """
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createSearchHistoryTable = """
            CREATE TABLE IF NOT EXISTS search_history (
                search_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                movie_title VARCHAR(255) NOT NULL,
                searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createRecHistoryTable = """
            CREATE TABLE IF NOT EXISTS recommendation_history (
                recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                search_id BIGINT NOT NULL,
                recommended_movie_id INT NOT NULL,
                similarity_score DECIMAL(5, 4) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_rec_search
                    FOREIGN KEY (search_id) REFERENCES search_history(search_id)
                    ON DELETE CASCADE,
                CONSTRAINT fk_rec_movie
                    FOREIGN KEY (recommended_movie_id) REFERENCES movies(movie_id)
                    ON DELETE CASCADE
            )
            """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createMoviesTable);
            statement.execute(createSearchHistoryTable);
            statement.execute(createRecHistoryTable);

            logger.info("MySQL Database Schema (movies, search_history, recommendation_history) verified.");

            // Seed initial movies if table is empty
            if (movieRepository.count() == 0) {
                List<Movie> initialMovies = tmdbApiClient.getFallbackDataset();
                movieRepository.saveOrUpdateAll(initialMovies);
                logger.info("Seeded MySQL 'movies' table with {} initial movies.", initialMovies.size());
            }

        } catch (SQLException e) {
            logger.warn("Database initialization notice: {}", e.getMessage());
        }
    }
}
