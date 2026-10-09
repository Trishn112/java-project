package com.recommendation.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a recorded search query and its generated recommendations in MySQL.
 * Demonstrates JDBC ENTITY MAPPING and COMPOSITION.
 */
public class SearchHistory {

    private long searchId;
    private String movieTitle;
    private LocalDateTime searchedAt;
    private int resultCount;
    private List<MovieRecommendation> recommendations;

    public SearchHistory() {
        this.recommendations = new ArrayList<>();
        this.searchedAt = LocalDateTime.now();
    }

    public SearchHistory(long searchId, String movieTitle, LocalDateTime searchedAt) {
        this.searchId = searchId;
        this.movieTitle = movieTitle;
        this.searchedAt = searchedAt != null ? searchedAt : LocalDateTime.now();
        this.recommendations = new ArrayList<>();
    }

    public SearchHistory(long searchId, String queryTitle, int targetMovieId, String targetMovieTitle,
                         int resultCount, LocalDateTime searchedAt) {
        this(searchId, queryTitle != null ? queryTitle : targetMovieTitle, searchedAt);
        this.resultCount = resultCount;
    }

    public long getSearchId() {
        return searchId;
    }

    public void setSearchId(long searchId) {
        this.searchId = searchId;
    }

    public long getId() {
        return searchId;
    }

    public void setId(long id) {
        this.searchId = id;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getQueryTitle() {
        return movieTitle;
    }

    public void setQueryTitle(String queryTitle) {
        this.movieTitle = queryTitle;
    }

    public String getTargetMovieTitle() {
        return movieTitle;
    }

    public int getResultCount() {
        if (resultCount > 0) {
            return resultCount;
        }
        return recommendations != null ? recommendations.size() : 0;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }

    public List<MovieRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<MovieRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }
}
