package com.recommendation.service;

import com.recommendation.model.Movie;
import com.recommendation.model.MovieRecommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO containing the searched target movie, computed recommendations,
 * data source attribution (Live TMDB API or Fallback), and performance metrics.
 * Demonstrates ENCAPSULATION and DTO pattern.
 */
public class MovieRecommendationResult {

    private final Movie targetMovie;
    private final List<MovieRecommendation> recommendations;
    private final String dataSource;
    private final long executionTimeMs;

    public MovieRecommendationResult(Movie targetMovie, List<MovieRecommendation> recommendations,
                                     String dataSource, long executionTimeMs) {
        this.targetMovie = targetMovie;
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        this.dataSource = dataSource != null ? dataSource : "TMDB API";
        this.executionTimeMs = executionTimeMs;
    }

    public Movie getTargetMovie() {
        return targetMovie;
    }

    public List<MovieRecommendation> getRecommendations() {
        return recommendations;
    }

    public String getDataSource() {
        return dataSource;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getResultCount() {
        return recommendations.size();
    }
}
