package com.recommendation;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import com.recommendation.model.SearchHistory;
import com.recommendation.repository.MovieRepository;
import com.recommendation.repository.SearchHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Tests for MySQL Database Connectivity, CRUD Operations,
 * Duplicate Prevention, SQL Joins, and Transactions.
 */
@SpringBootTest
class MovieDatabaseTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Test
    @DisplayName("1. Verify Database Connection from DataSource")
    void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "DataSource bean must be configured");
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn, "Connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be open");
            assertTrue(conn.isValid(2), "Database connection should be valid");
        }
    }

    @Test
    @DisplayName("2. Test Movie Persistence & Duplicate Prevention (Upsert)")
    void testMovieUpsertNoDuplicates() {
        int testTmdbId = 980000 + (int)(System.currentTimeMillis() % 10000);
        int initialCount = movieRepository.count();

        // 1. Create test movie
        Movie movie1 = new Movie(testTmdbId, "Test Film Alpha", "Overview of test film", "2024-01-01", 2024,
                Arrays.asList("Action", "Sci-Fi"), Collections.emptyList(), Collections.emptyList(),
                "Test Director", 7.5, 100, "/test_alpha.jpg");

        Movie saved1 = movieRepository.saveOrUpdate(movie1);
        assertNotNull(saved1);
        assertTrue(saved1.getMovieId() > 0, "Saved movie should receive a database primary key movie_id");

        // 2. Query back by TMDB ID
        Optional<Movie> foundOpt = movieRepository.findByTmdbId(testTmdbId);
        assertTrue(foundOpt.isPresent(), "Should find movie by tmdb_id");
        assertEquals("Test Film Alpha", foundOpt.get().getTitle());
        assertEquals(7.5, foundOpt.get().getVoteAverage());

        // 3. Update the movie with new rating and verify no duplicate record is created
        Movie updatedMovie = new Movie(testTmdbId, "Test Film Alpha (Updated)", "Updated Overview", "2024-01-01", 2024,
                Arrays.asList("Action", "Sci-Fi", "Thriller"), Collections.emptyList(), Collections.emptyList(),
                "Test Director", 8.8, 250, "/test_alpha.jpg");

        Movie saved2 = movieRepository.saveOrUpdate(updatedMovie);
        assertEquals(saved1.getMovieId(), saved2.getMovieId(), "Updated movie must retain the same movie_id primary key");

        Optional<Movie> updatedOpt = movieRepository.findByTmdbId(testTmdbId);
        assertTrue(updatedOpt.isPresent());
        assertEquals("Test Film Alpha (Updated)", updatedOpt.get().getTitle());
        assertEquals(8.8, updatedOpt.get().getVoteAverage());
        assertEquals(250, updatedOpt.get().getVoteCount());

        // Count should have increased by exactly 1 (no duplicate rows created despite update)
        assertEquals(initialCount + 1, movieRepository.count());
    }

    @Test
    @DisplayName("3. Test Search History & Recommendation History Transactional Save")
    void testTransactionalSearchHistoryLogging() {
        int testTmdbIdA = 990000 + (int)(System.currentTimeMillis() % 10000);
        int testTmdbIdB = testTmdbIdA + 1;

        Movie target = new Movie(testTmdbIdA, "Target Sci-Fi", "Description", "2024", 2024,
                Arrays.asList("Sci-Fi"), Collections.emptyList(), Collections.emptyList(), "Director", 8.0, 50, "");
        Movie candidate = new Movie(testTmdbIdB, "Similar Sci-Fi", "Description", "2024", 2024,
                Arrays.asList("Sci-Fi"), Collections.emptyList(), Collections.emptyList(), "Director", 8.2, 80, "");

        movieRepository.saveOrUpdate(target);
        movieRepository.saveOrUpdate(candidate);

        MovieRecommendation rec = new MovieRecommendation(target, candidate, 0.85, 0.82, 0.841, 0.85, 0.0, 0.0);
        List<MovieRecommendation> recs = Collections.singletonList(rec);

        long searchId = searchHistoryRepository.saveSearchWithRecommendations("Target Sci-Fi", target, recs);
        assertTrue(searchId > 0, "Search history transaction must return valid positive search_id");

        // Test SQL JOIN retrieval
        List<MovieRecommendation> retrievedRecs = searchHistoryRepository.findRecommendationsForSearchId(searchId);
        assertNotNull(retrievedRecs);
        assertEquals(1, retrievedRecs.size(), "SQL JOIN should return 1 recommendation for search_id");
        assertEquals("Similar Sci-Fi", retrievedRecs.get(0).getMovie().getTitle());
        assertEquals(0.85, retrievedRecs.get(0).getSimilarityScore(), 0.001);

        // Test Recent Searches Query
        List<SearchHistory> recent = searchHistoryRepository.findRecentSearches(5);
        assertFalse(recent.isEmpty());
        assertEquals("Target Sci-Fi", recent.get(0).getMovieTitle());

        // Clean up test search record
        searchHistoryRepository.deleteSearchById(searchId);
    }
}
