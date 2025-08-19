package com.ucmp.ucmp_backend.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
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
    @Column(name="profile_id")
    private Long profileId;


//    @Column(name = "college_id")
//    private String collegeId;

    @Column(name="name")
    private String name;

    @Email
    private String email;



}
