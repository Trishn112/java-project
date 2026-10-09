package com.recommendation.model;

/**
 * Represents an item to be recommended in the system.
 * Demonstrates COMPOSITION / ASSOCIATION by holding a reference to a Category object (Item HAS-A Category).
 */
public class Item {
    private int id;
    private String name;
    private String description;
    private Category category;
    private double rating;
    private int popularity;

    /**
     * Default constructor.
     */
    public Item() {
    }

    /**
     * Parameterized constructor.
     *
     * @param id          Unique identifier for the item
     * @param name        Name/Title of the item
     * @param description Brief description of the item
     * @param category    Category object associated with the item (HAS-A relationship)
     * @param rating      Average rating of the item
     * @param popularity  Popularity counter / interaction score
     */
    public Item(int id, String name, String description, Category category, double rating, int popularity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rating = rating;
        this.popularity = popularity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id && java.util.Objects.equals(name, item.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category=" + (category != null ? category.getName() : "None") +
                ", rating=" + rating +
                ", popularity=" + popularity +
                '}';
    }
}
