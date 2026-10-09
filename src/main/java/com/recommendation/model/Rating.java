package com.recommendation.model;

/**
 * Represents a rating given by a User to an Item.
 * Demonstrates COMPOSITION / ASSOCIATION (Rating HAS-A User, Rating HAS-A Item)
 * and DATA VALIDATION with exception handling.
 */
public class Rating {
    private int id;
    private User user;
    private Item item;
    private double rating;

    /**
     * Default constructor.
     */
    public Rating() {
    }

    /**
     * Parameterized constructor with validation.
     *
     * @param id     Unique identifier for the rating
     * @param user   User who gave the rating
     * @param item   Item that was rated
     * @param rating Numeric score between 1.0 and 5.0
     * @throws IllegalArgumentException if rating is not between 1.0 and 5.0
     */
    public Rating(int id, User user, Item item, double rating) {
        validateRating(rating);
        this.id = id;
        this.user = user;
        this.item = item;
        this.rating = rating;
    }

    /**
     * Helper method to validate rating range.
     *
     * @param rating Score to validate
     * @throws IllegalArgumentException if rating is outside [1.0, 5.0]
     */
    private void validateRating(double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Invalid rating: " + rating + ". Rating must be between 1.0 and 5.0.");
        }
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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        validateRating(rating);
        this.rating = rating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rating rating1 = (Rating) o;
        return id == rating1.id && java.util.Objects.equals(user, rating1.user) && java.util.Objects.equals(item, rating1.item);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, user, item);
    }

    @Override
    public String toString() {
        return "Rating{" +
                "id=" + id +
                ", user=" + (user != null ? user.getName() : "None") +
                ", item=" + (item != null ? item.getName() : "None") +
                ", rating=" + rating +
                '}';
    }
}
