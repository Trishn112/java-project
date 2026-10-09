package com.recommendation.exception;

/**
 * Thrown when a JDBC database access or transaction error occurs.
 * Demonstrates DATABASE EXCEPTION HANDLING and TRANSACTION ROLLBACK.
 */
public class DatabaseAccessException extends MovieRecommendationException {

    public DatabaseAccessException(String message) {
        super(message);
    }

    public DatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
