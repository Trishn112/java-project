package com.recommendation.repository;

import com.recommendation.model.Movie;

import java.util.List;
import java.util.Optional;

/**
 * Interface defining CRUD and caching contracts for Movie entities in MySQL.
 * Demonstrates the OOP principles of INTERFACES, ABSTRACTION, and REPOSITORY PATTERN.
 */
public interface MovieRepository {

    /**
     * Saves or updates a movie record in the database.
     * Prevents duplicates by matching on tmdb_id.
     *
     * @param movie Movie entity to save or update
     * @return Persisted Movie with assigned database movie_id
     */
    Movie saveOrUpdate(Movie movie);

    /**
     * Batch saves or updates multiple movies in a single operation.
     *
     * @param movies List of movies to persist
     * @return List of persisted movies with assigned movie_id
     */
    List<Movie> saveOrUpdateAll(List<Movie> movies);

    /**
     * Finds a cached movie by its unique TMDB external ID.
     *
     * @param tmdbId TMDB movie identifier
     * @return Optional containing Movie if found, empty otherwise
     */
    Optional<Movie> findByTmdbId(int tmdbId);

    /**
     * Finds a cached movie by its title (case-insensitive search).
     *
     * @param title Title of the movie
     * @return Optional containing Movie if found, empty otherwise
     */
    Optional<Movie> findByTitle(String title);

    /**
     * Retrieves all cached movies currently stored in the MySQL database.
     *
     * @return List of all stored movies
     */
    List<Movie> findAllCachedMovies();

    /**
     * Returns total count of cached movies in the database.
     * Demonstrates SQL COUNT(*) aggregation.
     *
     * @return total movie records count
     */
    int count();
}
