package com.recommendation.exception;

/**
 * Base custom runtime exception for the Movie Recommendation System.
 * Demonstrates EXCEPTION HANDLING and inheritance hierarchy.
 */
public class MovieRecommendationException extends RuntimeException {
    public MovieRecommendationException(String message) {
        super(message);
    }

    public MovieRecommendationException(String message, Throwable cause) {
        super(message, cause);
    }
}
