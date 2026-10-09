package com.recommendation.exception;

/**
 * Thrown when an error occurs while communicating with the TMDB API.
 * Demonstrates API EXCEPTION HANDLING.
 */
public class TmdbApiException extends MovieRecommendationException {

    private final int statusCode;

    public TmdbApiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public TmdbApiException(String message, int statusCode) {
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }

    public TmdbApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
