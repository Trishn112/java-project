package com.recommendation.model;

/**
 * Represents a standard User in the recommendation system.
 * Demonstrates the principle of ENCAPSULATION using private fields
 * and public getters and setters.
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String role;

    /**
     * Default constructor.
     */
    public User() {
        this.role = "USER";
    }

    /**
     * Parameterized constructor without explicit role (defaults to USER).
     *
     * @param id       Unique identifier for the user
     * @param name     Full name of the user
     * @param email    Email address of the user
     * @param password Account password
     */
    public User(int id, String name, String email, String password) {
        this(id, name, email, password, "USER");
    }

    /**
     * Full parameterized constructor.
     *
     * @param id       Unique identifier for the user
     * @param name     Full name of the user
     * @param email    Email address of the user
     * @param password Account password
     * @param role     User role (e.g., "USER", "ADMIN")
     */
    public User(int id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // --- Getters and Setters demonstrating Encapsulation ---

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && java.util.Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
