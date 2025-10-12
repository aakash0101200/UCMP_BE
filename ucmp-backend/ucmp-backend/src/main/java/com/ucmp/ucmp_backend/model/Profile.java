package com.ucmp.ucmp_backend.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Entity
@Table(name = "profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private  String name;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;
    private String address;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    private String User_CollegeId;
}
