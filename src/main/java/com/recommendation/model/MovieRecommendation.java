package com.recommendation.model;

import java.util.Objects;

/**
 * Represents a ranked movie recommendation with granular similarity metrics.
 * Demonstrates ENCAPSULATION and COMPOSITION (MovieRecommendation HAS-A Movie).
 */
public class MovieRecommendation {

    private Movie targetMovie;
    private Movie movie;
    private double similarityScore;       // Content similarity [0.0 - 1.0]
    private double normalizedRating;      // Normalized TMDB rating [0.0 - 1.0]
    private double finalScore;            // Composite ranking score [0.0 - 1.0]

    // Component breakdown metrics for explainability in viva
    private double genreSimilarity;
    private double overviewSimilarity;
    private double castDirectorSimilarity;

    public MovieRecommendation() {
    }

    public MovieRecommendation(Movie targetMovie, Movie movie, double similarityScore,
                               double normalizedRating, double finalScore,
                               double genreSimilarity, double overviewSimilarity,
                               double castDirectorSimilarity) {
        this.targetMovie = targetMovie;
        this.movie = movie;
        this.similarityScore = Math.min(Math.max(similarityScore, 0.0), 1.0);
        this.normalizedRating = Math.min(Math.max(normalizedRating, 0.0), 1.0);
        this.finalScore = Math.min(Math.max(finalScore, 0.0), 1.0);
        this.genreSimilarity = Math.min(Math.max(genreSimilarity, 0.0), 1.0);
        this.overviewSimilarity = Math.min(Math.max(overviewSimilarity, 0.0), 1.0);
        this.castDirectorSimilarity = Math.min(Math.max(castDirectorSimilarity, 0.0), 1.0);
    }

    public Movie getTargetMovie() {
        return targetMovie;
    }

    public void setTargetMovie(Movie targetMovie) {
        this.targetMovie = targetMovie;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public double getNormalizedRating() {
        return normalizedRating;
    }

    public void setNormalizedRating(double normalizedRating) {
        this.normalizedRating = normalizedRating;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public double getGenreSimilarity() {
        return genreSimilarity;
    }

    public void setGenreSimilarity(double genreSimilarity) {
        this.genreSimilarity = genreSimilarity;
    }

    public double getOverviewSimilarity() {
        return overviewSimilarity;
    }

    public void setOverviewSimilarity(double overviewSimilarity) {
        this.overviewSimilarity = overviewSimilarity;
    }

    public double getCastDirectorSimilarity() {
        return castDirectorSimilarity;
    }

    public void setCastDirectorSimilarity(double castDirectorSimilarity) {
        this.castDirectorSimilarity = castDirectorSimilarity;
    }

    /**
     * Formats the content similarity as an integer percentage, e.g. 88%.
     */
    public String getSimilarityPercentageFormatted() {
        return String.format("%.0f%%", similarityScore * 100.0);
    }

    /**
     * Formats the rating out of 10.
     */
    public String getRatingFormatted() {
        if (movie == null) return "N/A";
        return String.format("%.1f / 10", movie.getVoteAverage());
    }

    /**
     * Formats the final composite score to 3 decimal places.
     */
    public String getFinalScoreFormatted() {
        return String.format("%.3f", finalScore);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieRecommendation that = (MovieRecommendation) o;
        return Double.compare(that.finalScore, finalScore) == 0 &&
                Objects.equals(targetMovie, that.targetMovie) &&
                Objects.equals(movie, that.movie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetMovie, movie, finalScore);
    }

    @Override
    public String toString() {
        return "MovieRecommendation{" +
                "movie=" + (movie != null ? movie.getTitle() : "null") +
                ", similarity=" + getSimilarityPercentageFormatted() +
                ", rating=" + getRatingFormatted() +
                ", finalScore=" + getFinalScoreFormatted() +
                '}';
    }
}
