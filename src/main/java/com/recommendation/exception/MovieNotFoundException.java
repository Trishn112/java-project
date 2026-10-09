package com.recommendation.exception;

/**
 * Thrown when a movie title is not found in TMDB API or local repository.
 * Demonstrates CUSTOM EXCEPTION HANDLING.
 */
public class MovieNotFoundException extends MovieRecommendationException {

    private final String movieTitle;

    public MovieNotFoundException(String movieTitle) {
        super("Movie not found: '" + movieTitle + "'. Please check the spelling or try another popular movie name.");
        this.movieTitle = movieTitle;
    }

    public MovieNotFoundException(String movieTitle, String customMessage) {
        super(customMessage);
        this.movieTitle = movieTitle;
    }

    public String getMovieTitle() {
        return movieTitle;
    }
}
