package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "collegeId")
})
@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@Inheritance(strategy = InheritanceType.JOINED) // This is key for the inheritance
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, updatable = false)
    private String collegeId; // e.g., "STU123", "FAC456"

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Email
    @Column(unique = true , nullable = false)
    private String email;

    @Column
    private String depaerment;

    @Column
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
//
//    @Column(nullable = false)
//    private boolean isVerified = false; // For email/account verification

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



//    // Constructor for registration
//    public User(String collegeId, String password, String name, String email, Role role) {
//        this.collegeId = collegeId;
//        this.password = password;
//        this.name = name;
//        this.email = email;
//        this.role = role;
//    }

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Profile profile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Student student;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Faculty faculty;



}
