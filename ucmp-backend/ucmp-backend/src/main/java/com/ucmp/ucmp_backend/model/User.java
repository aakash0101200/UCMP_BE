package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String collegeId;

    private String password;
    private String name;

    @Column(unique = true)
    private String email;   // NEW

    @Enumerated(EnumType.STRING)
    private Role role;

    // No-arg constructor
    public User() {}

    // All-args constructor
    public User(String collegeId, String password, String name, String email, Role role) {
        this.collegeId = collegeId;
        this.password = password;
        this.role = role;
        this.name = name;
        this.email = email;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getCollegeId() {
        return collegeId;
    }
    public void setCollegeId(String collegeId) {
        this.collegeId = collegeId;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public  String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }


    public enum Role {
        STUDENT, FACULTY, ADMIN
    }
}