package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
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


    // All-args constructor
    public User(String collegeId, String password, String name, String email, Role role) {
        this.collegeId = collegeId;
        this.password = password;
        this.role = role;
        this.name = name;
        this.email = email;
    }


    public enum Role {
        STUDENT, FACULTY, ADMIN
    }
}