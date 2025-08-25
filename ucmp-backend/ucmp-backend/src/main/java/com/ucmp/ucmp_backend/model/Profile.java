package com.ucmp.ucmp_backend.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;


@Entity
@Table(name = "profiles")
@AllArgsConstructor @NoArgsConstructor
@Getter
@Setter
public class Profile {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(unique = true, name = "college_id")
    private String collegeId;

    @Column(name="name")
    private String name;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Email
    private String email;

    @OneToOne //(mappedBy = "profile")
    @JsonIgnore //prevents Recursion
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;



}
