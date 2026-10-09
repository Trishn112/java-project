package com.recommendation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model class representing a paginated response of movies from TMDB API.
 * Demonstrates ENCAPSULATION, GENERICS, and PAGINATION METADATA.
 */
public class MoviePageResult {

    private List<Movie> movies;
    private int page;
    private int totalPages;
    private int totalResults;
    private String category;
    private String categoryTitle;
    private String region;
    private String language;
    private boolean fromCache;

    public MoviePageResult() {
        this.movies = new ArrayList<>();
        this.page = 1;
        this.totalPages = 1;
        this.totalResults = 0;
        this.category = "now_playing";
        this.categoryTitle = "Now Playing in Theaters";
        this.region = "";
        this.language = "";
        this.fromCache = false;
    }

    public MoviePageResult(List<Movie> movies, int page, int totalPages, int totalResults,
                           String category, String categoryTitle, String region, boolean fromCache) {
        this(movies, page, totalPages, totalResults, category, categoryTitle, region, "", fromCache);
    }

    public MoviePageResult(List<Movie> movies, int page, int totalPages, int totalResults,
                           String category, String categoryTitle, String region, String language, boolean fromCache) {
        this.movies = movies != null ? new ArrayList<>(movies) : new ArrayList<>();
        this.page = page;
        this.totalPages = Math.max(totalPages, 1);
        this.totalResults = totalResults;
        this.category = category;
        this.categoryTitle = categoryTitle;
        this.region = region != null ? region : "";
        this.language = language != null ? language : "";
        this.fromCache = fromCache;
    }

    public List<Movie> getMovies() {
        return Collections.unmodifiableList(movies);
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies != null ? new ArrayList<>(movies) : new ArrayList<>();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLanguage() {
        return language != null ? language : "";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public int getPreviousPage() {
        return Math.max(page - 1, 1);
    }

    public int getNextPage() {
        return Math.min(page + 1, totalPages);
    }

    public boolean isEmpty() {
        return movies == null || movies.isEmpty();
    }

    public int getResultCount() {
        return movies != null ? movies.size() : 0;
    }
}
