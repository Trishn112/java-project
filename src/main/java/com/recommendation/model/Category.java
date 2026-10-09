package com.recommendation.model;

import java.util.Objects;

/**
 * Represents an item category (e.g., Electronics, Books, Movies).
 * Demonstrates ENCAPSULATION.
 */
public class Category {
    private int id;
    private String name;

    /**
     * Default constructor.
     */
    public Category() {
    }

    /**
     * Parameterized constructor.
     *
     * @param id   Unique identifier for the category
     * @param name Name of the category
     */
    public Category(int id, String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
