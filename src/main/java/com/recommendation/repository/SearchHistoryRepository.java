package com.recommendation.repository;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import com.recommendation.model.SearchHistory;

import java.util.List;

/**
 * Interface defining transactional operations for Search History and Recommendation History.
 * Demonstrates INTERFACES, ABSTRACTION, and REPOSITORY PATTERN.
 */
public interface SearchHistoryRepository {

    /**
     * Transactionally saves a search query record and all generated recommendation history links.
     * Demonstrates EXPLICIT TRANSACTIONS (commit and rollback).
     *
     * @param movieTitle      Movie title entered by user
     * @param targetMovie     Matched target reference movie
     * @param recommendations Ranked list of generated recommendations
     * @return Generated search_id primary key
     */
    long saveSearchWithRecommendations(String movieTitle, Movie targetMovie, List<MovieRecommendation> recommendations);

    /**
     * Retrieves recent search history records ordered by searched_at descending.
     *
     * @param limit Maximum number of records to return
     * @return List of SearchHistory entities
     */
    List<SearchHistory> findRecentSearches(int limit);

    /**
     * Retrieves all recommendations associated with a specific search query using a SQL JOIN.
     * Demonstrates SQL INNER JOIN between recommendation_history and movies tables.
     *
     * @param searchId The primary key search_id
     * @return List of MovieRecommendation objects populated via SQL JOIN
     */
    List<MovieRecommendation> findRecommendationsForSearchId(long searchId);

    /**
     * Counts the total number of searches executed.
     * Demonstrates SQL aggregate COUNT(*).
     *
     * @return total search count
     */
    long countSearches();

    /**
     * Deletes a search record by ID. Foreign key cascade deletes linked recommendations.
     * Demonstrates Foreign Key ON DELETE CASCADE in MySQL.
     *
     * @param searchId ID of the search history record to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteSearchById(long searchId);
}
