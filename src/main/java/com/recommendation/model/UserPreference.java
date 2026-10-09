package com.recommendation.model;

/**
 * Represents a user's affinity or preference score for a specific category.
 * Demonstrates COMPOSITION / ASSOCIATION (UserPreference HAS-A User, UserPreference HAS-A Category).
 */
public class UserPreference {
    private int id;
    private User user;
    private Category category;
    private double preferenceScore;

    /**
     * Default constructor.
     */
    public UserPreference() {
    }

    /**
     * Parameterized constructor.
     *
     * @param id              Unique identifier for the user preference record
     * @param user            User associated with this preference
     * @param category        Category of interest
     * @param preferenceScore Quantified affinity score (e.g. 0.0 to 1.0 or weights)
     */
    public UserPreference(int id, User user, Category category, double preferenceScore) {
        this.id = id;
        this.user = user;
        this.category = category;
        this.preferenceScore = preferenceScore;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getPreferenceScore() {
        return preferenceScore;
    }

    public void setPreferenceScore(double preferenceScore) {
        this.preferenceScore = preferenceScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return id == that.id && java.util.Objects.equals(user, that.user) && java.util.Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, user, category);
    }

    @Override
    public String toString() {
        return "UserPreference{" +
                "id=" + id +
                ", user=" + (user != null ? user.getName() : "None") +
                ", category=" + (category != null ? category.getName() : "None") +
                ", preferenceScore=" + preferenceScore +
                '}';
    }
}
