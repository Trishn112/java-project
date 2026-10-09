package com.recommendation.repository;

import com.recommendation.exception.DatabaseAccessException;
import com.recommendation.model.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of {@link MovieRepository} for MySQL 8.0+.
 * Demonstrates:
 * 1. JDBC Connections, PreparedStatements, and ResultSets
 * 2. SQL Injection Prevention via parameterized queries
 * 3. Upsert logic (insert or update on duplicate tmdb_id)
 * 4. SQL Aggregates (COUNT), Filtering (WHERE), and Sorting (ORDER BY)
 * 5. Exception handling with DatabaseAccessException
 */
@Repository
public class MovieJdbcRepository implements MovieRepository {

    private static final Logger logger = LoggerFactory.getLogger(MovieJdbcRepository.class);
    private final DataSource dataSource;

    public MovieJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Movie saveOrUpdate(Movie movie) {
        if (movie == null || movie.getTmdbId() <= 0) {
            return movie;
        }

        // 1. Check if movie already exists by tmdb_id
        Optional<Movie> existingOpt = findByTmdbId(movie.getTmdbId());
        if (existingOpt.isPresent()) {
            Movie existing = existingOpt.get();
            movie.setMovieId(existing.getMovieId());

            String updateSql = """
                UPDATE movies 
                SET title = ?, release_year = ?, overview = ?, genres = ?, 
                    poster_path = ?, rating = ?, vote_count = ?
                WHERE tmdb_id = ?
                """;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                ps.setString(1, movie.getTitle());
                if (movie.getReleaseYear() > 0) {
                    ps.setInt(2, movie.getReleaseYear());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setString(3, movie.getOverview());
                ps.setString(4, movie.getGenreString());
                ps.setString(5, movie.getPosterPath());
                ps.setDouble(6, movie.getVoteAverage());
                ps.setInt(7, movie.getVoteCount());
                ps.setInt(8, movie.getTmdbId());

                ps.executeUpdate();
                logger.debug("Updated existing movie record in MySQL: '{}' (ID: {})", movie.getTitle(), movie.getMovieId());
                return movie;

            } catch (SQLException e) {
                logger.error("Failed to update movie in MySQL: {}", e.getMessage(), e);
                throw new DatabaseAccessException("Error updating movie record: " + e.getMessage(), e);
            }
        }

        // 2. Insert new movie record
        String insertSql = """
            INSERT INTO movies (tmdb_id, title, release_year, overview, genres, poster_path, rating, vote_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movie.getTmdbId());
            ps.setString(2, movie.getTitle());
            if (movie.getReleaseYear() > 0) {
                ps.setInt(3, movie.getReleaseYear());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, movie.getOverview());
            ps.setString(5, movie.getGenreString());
            ps.setString(6, movie.getPosterPath());
            ps.setDouble(7, movie.getVoteAverage());
            ps.setInt(8, movie.getVoteCount());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    movie.setMovieId(keys.getInt(1));
                }
            }

            logger.debug("Inserted new movie record into MySQL: '{}' (ID: {})", movie.getTitle(), movie.getMovieId());
            return movie;

        } catch (SQLException e) {
            logger.error("Failed to insert movie into MySQL: {}", e.getMessage(), e);
            throw new DatabaseAccessException("Error inserting movie record: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Movie> saveOrUpdateAll(List<Movie> movies) {
        List<Movie> savedList = new ArrayList<>();
        if (movies == null || movies.isEmpty()) {
            return savedList;
        }

        for (Movie m : movies) {
            try {
                savedList.add(saveOrUpdate(m));
            } catch (Exception e) {
                logger.warn("Skipping persistence for movie '{}': {}", m.getTitle(), e.getMessage());
            }
        }
        return savedList;
    }

    @Override
    public Optional<Movie> findByTmdbId(int tmdbId) {
        String sql = "SELECT movie_id, tmdb_id, title, release_year, overview, genres, poster_path, rating, vote_count FROM movies WHERE tmdb_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tmdbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding movie by TMDB ID {}: {}", tmdbId, e.getMessage());
            throw new DatabaseAccessException("Error querying movie by tmdb_id: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Movie> findByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT movie_id, tmdb_id, title, release_year, overview, genres, poster_path, rating, vote_count FROM movies WHERE LOWER(title) = LOWER(?) LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding movie by title '{}': {}", title, e.getMessage());
            throw new DatabaseAccessException("Error querying movie by title: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public List<Movie> findAllCachedMovies() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT movie_id, tmdb_id, title, release_year, overview, genres, poster_path, rating, vote_count FROM movies ORDER BY rating DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToMovie(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all cached movies from MySQL: {}", e.getMessage());
            throw new DatabaseAccessException("Error retrieving cached movies: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM movies";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting movies in MySQL: {}", e.getMessage());
            throw new DatabaseAccessException("Error executing COUNT query on movies: " + e.getMessage(), e);
        }
        return 0;
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
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
        return movie;
    }
}
