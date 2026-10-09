package com.recommendation.model;

import java.util.Objects;

/**
 * Represents a recommendation output containing the target user, recommended item,
 * computed recommendation confidence score, and granular scoring breakdown components.
 */
public class Recommendation {
    private User user;
    private Item item;
    private double score;

    // Granular scoring components for explainability / viva verification
    private double preferenceScore;
    private double categoryScore;
    private double ratingScore;
    private double popularityScore;

    /**
     * Default constructor.
     */
    public Recommendation() {
    }

    /**
     * Parameterized constructor (Phase 1 & 2 compatible).
     *
     * @param user  Target user for the recommendation
     * @param item  Recommended item
     * @param score Calculated composite score for the recommendation
     */
    public Recommendation(User user, Item item, double score) {
        this.user = user;
        this.item = item;
        this.score = score;
    }

    /**
     * Full parameterized constructor including individual scoring components (Phase 3).
     *
     * @param user            Target user
     * @param item            Recommended item
     * @param score           Composite weighted score
     * @param preferenceScore Normalized user preference score [0.0 - 1.0]
     * @param categoryScore   Normalized category match score [0.0 - 1.0]
     * @param ratingScore     Normalized item rating score [0.0 - 1.0]
     * @param popularityScore Normalized item popularity score [0.0 - 1.0]
     */
    public Recommendation(User user, Item item, double score,
                          double preferenceScore, double categoryScore,
                          double ratingScore, double popularityScore) {
        this.user = user;
        this.item = item;
        this.score = score;
        this.preferenceScore = preferenceScore;
        this.categoryScore = categoryScore;
        this.ratingScore = ratingScore;
        this.popularityScore = popularityScore;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getPreferenceScore() {
        return preferenceScore;
    }

    public void setPreferenceScore(double preferenceScore) {
        this.preferenceScore = preferenceScore;
    }

    public double getCategoryScore() {
        return categoryScore;
    }

    public void setCategoryScore(double categoryScore) {
        this.categoryScore = categoryScore;
    }

    public double getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(double ratingScore) {
        this.ratingScore = ratingScore;
    }

    public double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(double popularityScore) {
        this.popularityScore = popularityScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recommendation that = (Recommendation) o;
        return Double.compare(that.score, score) == 0 &&
                Objects.equals(user, that.user) &&
                Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, item, score);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "user=" + (user != null ? user.getName() : "None") +
                ", item=" + (item != null ? item.getName() : "None") +
                ", score=" + String.format("%.2f", score) +
                '}';
    }
}
