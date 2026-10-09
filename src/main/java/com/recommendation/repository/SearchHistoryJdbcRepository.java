package com.recommendation.repository;

import com.recommendation.exception.DatabaseAccessException;
import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;
import com.recommendation.model.SearchHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation of {@link SearchHistoryRepository} for MySQL 8.0+.
 * Demonstrates:
 * 1. JDBC DataSource, Connection, PreparedStatement, ResultSet
 * 2. EXPLICIT TRANSACTIONS: setAutoCommit(false), commit(), and rollback()
 * 3. SQL Relational Joins (INNER JOIN between recommendation_history and movies)
 * 4. SQL Aggregation (COUNT) and Foreign Key Cascade Deletion
 * 5. Robust Exception Handling with DatabaseAccessException
 */
@Repository
public class SearchHistoryJdbcRepository implements SearchHistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(SearchHistoryJdbcRepository.class);

    private final DataSource dataSource;
    private final MovieRepository movieRepository;

    public SearchHistoryJdbcRepository(DataSource dataSource, MovieRepository movieRepository) {
        this.dataSource = dataSource;
        this.movieRepository = movieRepository;
    }

    @Override
    public long saveSearchWithRecommendations(String movieTitle, Movie targetMovie, List<MovieRecommendation> recommendations) {
        if (movieTitle == null || movieTitle.trim().isEmpty() || recommendations == null) {
            return -1;
        }

        // First ensure candidate movies are saved in `movies` table so they have valid movie_id foreign keys
        for (MovieRecommendation rec : recommendations) {
            if (rec.getMovie() != null && rec.getMovie().getMovieId() <= 0) {
                movieRepository.saveOrUpdate(rec.getMovie());
            }
        }
        if (targetMovie != null && targetMovie.getMovieId() <= 0) {
            movieRepository.saveOrUpdate(targetMovie);
        }

        String insertSearchSql = "INSERT INTO search_history (movie_title, searched_at) VALUES (?, ?)";
        String insertRecHistorySql = """
            INSERT INTO recommendation_history (search_id, recommended_movie_id, similarity_score, created_at)
            VALUES (?, ?, ?, ?)
            """;

        Connection conn = null;
        long generatedSearchId = -1;

        try {
            conn = dataSource.getConnection();

            // 1. BEGIN TRANSACTION: Disable auto-commit
            conn.setAutoCommit(false);

            // 2. Insert into search_history
            try (PreparedStatement searchStmt = conn.prepareStatement(insertSearchSql, Statement.RETURN_GENERATED_KEYS)) {
                searchStmt.setString(1, movieTitle.trim());
                searchStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

                int rows = searchStmt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Creating search_history entry failed, no rows affected.");
                }

                try (ResultSet keys = searchStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedSearchId = keys.getLong(1);
                    } else {
                        throw new SQLException("Failed to obtain generated search_id key.");
                    }
                }
            }

            // 3. Batch insert into recommendation_history
            try (PreparedStatement recStmt = conn.prepareStatement(insertRecHistorySql)) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                for (MovieRecommendation rec : recommendations) {
                    Movie cand = rec.getMovie();
                    if (cand != null && cand.getMovieId() > 0) {
                        recStmt.setLong(1, generatedSearchId);
                        recStmt.setInt(2, cand.getMovieId());
                        recStmt.setDouble(3, rec.getSimilarityScore());
                        recStmt.setTimestamp(4, now);
                        recStmt.addBatch();
                    }
                }
                recStmt.executeBatch();
            }

            // 4. COMMIT TRANSACTION
            conn.commit();
            logger.info("MySQL Transaction Committed: search_id #{} logged with {} recommendations",
                    generatedSearchId, recommendations.size());

        } catch (SQLException e) {
            // 5. ROLLBACK TRANSACTION on failure
            if (conn != null) {
                try {
                    logger.warn("SQL Exception during search logging. Rolling back transaction: {}", e.getMessage());
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback MySQL transaction: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            throw new DatabaseAccessException("Transaction rolled back: " + e.getMessage(), e);
        } finally {
            // 6. Reset auto-commit and close connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error resetting connection auto-commit: {}", e.getMessage());
                }
            }
        }

        return generatedSearchId;
    }

    @Override
    public List<SearchHistory> findRecentSearches(int limit) {
        List<SearchHistory> list = new ArrayList<>();
        String sql = """
            SELECT sh.search_id, sh.movie_title, sh.searched_at, COUNT(rh.recommendation_id) AS result_count
            FROM search_history sh
            LEFT JOIN recommendation_history rh ON sh.search_id = rh.search_id
            GROUP BY sh.search_id, sh.movie_title, sh.searched_at
            ORDER BY sh.search_id DESC
            LIMIT ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit <= 0 ? 5 : limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("search_id");
                    String title = rs.getString("movie_title");
                    Timestamp ts = rs.getTimestamp("searched_at");
                    int resultCount = rs.getInt("result_count");
                    LocalDateTime time = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    SearchHistory history = new SearchHistory(id, title, time);
                    history.setResultCount(resultCount);
                    list.add(history);
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not retrieve recent searches from MySQL search_history: {}", e.getMessage());
        }

        return list;
    }

    public List<SearchHistory> getRecentSearches(int limit) {
        return findRecentSearches(limit);
    }

    @Override
    public List<MovieRecommendation> findRecommendationsForSearchId(long searchId) {
        List<MovieRecommendation> list = new ArrayList<>();

        // SQL Relational JOIN Query demonstrating SQL INNER JOIN between recommendation_history and movies
        String sql = """
            SELECT rh.recommendation_id, rh.search_id, rh.similarity_score,
                   m.movie_id, m.tmdb_id, m.title, m.release_year, m.overview, m.genres, 
                   m.poster_path, m.rating, m.vote_count
            FROM recommendation_history rh
            INNER JOIN movies m ON rh.recommended_movie_id = m.movie_id
            WHERE rh.search_id = ?
            ORDER BY rh.similarity_score DESC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, searchId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double similarityScore = rs.getDouble("similarity_score");
                    int movieId = rs.getInt("movie_id");
                    int tmdbId = rs.getInt("tmdb_id");
                    String title = rs.getString("title");
                    int releaseYear = rs.getInt("release_year");
                    String overview = rs.getString("overview");
                    String genresStr = rs.getString("genres");
                    String posterPath = rs.getString("poster_path");
                    double rating = rs.getDouble("rating");
                    int voteCount = rs.getInt("vote_count");

                    Movie movie = new Movie(movieId, tmdbId, title, overview, String.valueOf(releaseYear), releaseYear,
                            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", rating, voteCount, posterPath);
                    movie.setGenreString(genresStr);

                    double normRating = Math.min(Math.max(rating / 10.0, 0.0), 1.0);
                    double finalScore = (similarityScore * 0.70) + (normRating * 0.30);

                    MovieRecommendation rec = new MovieRecommendation(null, movie, similarityScore, normRating, finalScore,
                            similarityScore, 0.0, 0.0);
                    list.add(rec);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL JOIN for search_id #{}: {}", searchId, e.getMessage());
            throw new DatabaseAccessException("Error retrieving recommendations by SQL JOIN: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public long countSearches() {
        String sql = "SELECT COUNT(*) FROM search_history";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting search history records in MySQL: {}", e.getMessage());
            throw new DatabaseAccessException("Error executing COUNT query on search_history: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public boolean deleteSearchById(long searchId) {
        String sql = "DELETE FROM search_history WHERE search_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, searchId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting search record #{} from MySQL: {}", searchId, e.getMessage());
            throw new DatabaseAccessException("Error executing DELETE on search_history: " + e.getMessage(), e);
        }
    }
}
