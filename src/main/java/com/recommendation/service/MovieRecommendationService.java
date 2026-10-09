package com.recommendation.service;

import com.recommendation.client.TmdbApiClient;
import com.recommendation.engine.MovieRecommender;
import com.recommendation.exception.MovieNotFoundException;
import com.recommendation.model.Movie;
import com.recommendation.model.MoviePageResult;
import com.recommendation.model.MovieRecommendation;
import com.recommendation.model.SearchHistory;
import com.recommendation.repository.MovieRepository;
import com.recommendation.repository.SearchHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Core Service orchestrating Movie Search, MySQL Caching, AI Recommendation Computation,
 * and JDBC Transactional Logging.
 * Demonstrates COMPOSITION, DEPENDENCY INJECTION, and SERVICE/REPOSITORY SEPARATION.
 */
@Service
public class MovieRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(MovieRecommendationService.class);

    private final TmdbApiClient tmdbApiClient;
    private final MovieRecommender recommender;
    private final MovieRepository movieRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final int defaultTopN;

    public MovieRecommendationService(TmdbApiClient tmdbApiClient,
                                      MovieRecommender recommender,
                                      MovieRepository movieRepository,
                                      SearchHistoryRepository searchHistoryRepository,
                                      @Value("${recommendation.top-n:10}") int defaultTopN) {
        this.tmdbApiClient = tmdbApiClient;
        this.recommender = recommender;
        this.movieRepository = movieRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.defaultTopN = defaultTopN > 0 ? defaultTopN : 10;
    }

    /**
     * Executes the end-to-end recommendation workflow:
     * 1. Finds target movie (checking MySQL cache / TMDB API).
     * 2. Persists / updates target movie in MySQL 'movies' table (preventing duplicates).
     * 3. Multithreaded retrieval of candidate movies from TMDB / MySQL cache.
     * 4. Persists candidate movies in MySQL.
     * 5. Computes Content-Based Recommendation Ranking in pure Java.
     * 6. Transactionally logs search and recommendations into 'search_history' & 'recommendation_history'.
     *
     * @param queryTitle     The movie title entered by user
     * @param requestedTopN  Optional custom limit of recommendations
     * @return MovieRecommendationResult containing target movie, ranked recommendations, and data source
     */
    public MovieRecommendationResult getRecommendationsForMovie(String queryTitle, Integer requestedTopN) {
        if (queryTitle == null || queryTitle.trim().isEmpty()) {
            throw new MovieNotFoundException("", "Please enter a movie title to search.");
        }

        long startTime = System.currentTimeMillis();
        int topN = (requestedTopN != null && requestedTopN > 0) ? requestedTopN : defaultTopN;
        String cleanTitle = queryTitle.trim();

        // 1. Search for target movie (TMDB API or Local MySQL Cache)
        Movie targetMovie = null;
        try {
            targetMovie = tmdbApiClient.searchMovie(cleanTitle);
        } catch (MovieNotFoundException e) {
            // Check MySQL cache by title
            Optional<Movie> dbMovie = movieRepository.findByTitle(cleanTitle);
            if (dbMovie.isPresent()) {
                targetMovie = dbMovie.get();
            } else {
                throw e;
            }
        }

        // 2. Persist or Update target movie in MySQL
        targetMovie = movieRepository.saveOrUpdate(targetMovie);

        // 3. Fetch candidate movies asynchronously
        List<Movie> candidatePool = tmdbApiClient.fetchCandidateMoviesAsync(targetMovie);

        // Supplement candidate pool with any other cached movies from MySQL
        List<Movie> dbCached = movieRepository.findAllCachedMovies();
        for (Movie dbm : dbCached) {
            if (dbm.getTmdbId() != targetMovie.getTmdbId() && !candidatePool.contains(dbm)) {
                candidatePool.add(dbm);
            }
        }

        // 4. Save/Update candidate pool in MySQL (prevents duplicates via tmdb_id unique constraint)
        movieRepository.saveOrUpdateAll(candidatePool);

        // 5. Compute recommendations using Java content-based algorithm
        List<MovieRecommendation> recommendations = recommender.recommendSimilar(targetMovie, candidatePool, topN);

        long executionTime = System.currentTimeMillis() - startTime;
        String dataSource = tmdbApiClient.isApiKeyConfigured() ? "TMDB Live API & MySQL" : "MySQL Local DB / Fallback";

        // 6. JDBC Transactional Search History & Recommendation History Logging
        try {
            searchHistoryRepository.saveSearchWithRecommendations(cleanTitle, targetMovie, recommendations);
        } catch (Exception e) {
            logger.warn("Search history database logging warning: {}", e.getMessage());
        }

        return new MovieRecommendationResult(targetMovie, recommendations, dataSource, executionTime);
    }

    /**
     * Searches for multiple matching movies by title (TMDB API / Local cache).
     *
     * @param queryTitle The search string entered by user
     * @return List of matching Movie objects
     */
    public List<Movie> searchMovies(String queryTitle) {
        if (queryTitle == null || queryTitle.trim().isEmpty()) {
            return List.of();
        }
        return tmdbApiClient.searchMovies(queryTitle.trim());
    }

    /**
     * Executes the recommendation workflow directly for a specific TMDB Movie ID.
     *
     * @param tmdbId        TMDB external movie ID
     * @param requestedTopN Optional custom limit of recommendations
     * @return MovieRecommendationResult containing target movie and ranked recommendations
     */
    public MovieRecommendationResult getRecommendationsByTmdbId(int tmdbId, Integer requestedTopN) {
        if (tmdbId <= 0) {
            throw new MovieNotFoundException("ID: " + tmdbId, "Invalid Movie ID provided.");
        }

        long startTime = System.currentTimeMillis();
        int topN = (requestedTopN != null && requestedTopN > 0) ? requestedTopN : defaultTopN;

        // 1. Fetch full details for target movie by TMDB ID
        Movie targetMovie = tmdbApiClient.getMovieDetails(tmdbId);
        if (targetMovie == null) {
            throw new MovieNotFoundException("Movie ID: " + tmdbId);
        }

        // 2. Persist in MySQL if DB is active
        try {
            targetMovie = movieRepository.saveOrUpdate(targetMovie);
        } catch (Exception e) {
            logger.debug("Database save warning: {}", e.getMessage());
        }

        // 3. Fetch candidate movies asynchronously
        List<Movie> candidatePool = tmdbApiClient.fetchCandidateMoviesAsync(targetMovie);

        try {
            List<Movie> dbCached = movieRepository.findAllCachedMovies();
            for (Movie dbm : dbCached) {
                if (dbm.getTmdbId() != targetMovie.getTmdbId() && !candidatePool.contains(dbm)) {
                    candidatePool.add(dbm);
                }
            }
            movieRepository.saveOrUpdateAll(candidatePool);
        } catch (Exception e) {
            logger.debug("Database candidate cache warning: {}", e.getMessage());
        }

        // 4. Compute recommendations using content-based algorithm
        List<MovieRecommendation> recommendations = recommender.recommendSimilar(targetMovie, candidatePool, topN);

        long executionTime = System.currentTimeMillis() - startTime;
        String dataSource = tmdbApiClient.isApiKeyConfigured() ? "TMDB Live API & Java Recommender" : "Java Recommender Engine / Fallback";

        // 5. Transactional Search History Logging
        try {
            searchHistoryRepository.saveSearchWithRecommendations(targetMovie.getTitle(), targetMovie, recommendations);
        } catch (Exception e) {
            logger.debug("Search history database logging warning: {}", e.getMessage());
        }

        return new MovieRecommendationResult(targetMovie, recommendations, dataSource, executionTime);
    }

    /**
     * Retrieves recent search history from MySQL database.
     */
    public List<SearchHistory> getRecentSearches(int limit) {
        return searchHistoryRepository.findRecentSearches(limit);
    }

    /**
     * Retrieves paginated movies for a latest category (Now Playing, Upcoming, Trending, Recently Released)
     * with regional filtering, language filtering, and in-memory caching.
     *
     * @param category Category identifier (now_playing, upcoming, trending, recently_released)
     * @param page     1-based page number
     * @param region   Optional region code (e.g., "IN" for India)
     * @return MoviePageResult containing paginated movie list and pagination metadata
     */
    public MoviePageResult getLatestMovies(String category, int page, String region) {
        return getLatestMovies(category, page, region, "");
    }

    public MoviePageResult getLatestMovies(String category, int page, String region, String language) {
        return tmdbApiClient.fetchMoviesByCategory(category, page, region, language);
    }

    /**
     * Retrieves recommendations for a previous search ID via SQL JOIN query.
     */
    public List<MovieRecommendation> getRecommendationsForSearchId(long searchId) {
        return searchHistoryRepository.findRecommendationsForSearchId(searchId);
    }

    public TmdbApiClient getTmdbApiClient() {
        return tmdbApiClient;
    }

    public MovieRecommender getRecommender() {
        return recommender;
    }

    public MovieRepository getMovieRepository() {
        return movieRepository;
    }

    public SearchHistoryRepository getSearchHistoryRepository() {
        return searchHistoryRepository;
    }
}
