package com.recommendation.model;

/**
 * Represents an Administrator in the system.
 * Demonstrates INHERITANCE by extending the User base class,
 * and METHOD OVERRIDING / POLYMORPHISM by overriding getRole() and toString().
 */
public class Admin extends User {
    private String department;

    /**
     * Default constructor.
     */
    public Admin() {
        super();
        setRole("ADMIN");
        this.department = "System Administration";
    }

    /**
     * Parameterized constructor.
     *
     * @param id         Unique identifier for the admin
     * @param name       Full name of the admin
     * @param email      Email address of the admin
     * @param password   Account password
     * @param department Department responsible for administration
     */
    public Admin(int id, String name, String email, String password, String department) {
        super(id, name, email, password, "ADMIN");
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Overrides getRole() from the User parent class.
     * Demonstrates method overriding and polymorphism.
     *
     * @return String indicating Admin role and assigned department
     */
    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}
