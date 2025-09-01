package com.ucmp.ucmp_backend.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;


@Entity
@Table(name = "profiles",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"})
        })
@AllArgsConstructor @NoArgsConstructor
@Getter
@Setter
public class Profile {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "Phone_number" )
    private String phoneNumber;

    private String address;

    @OneToOne
//    @MapsId   //ensure user_id == profile_Id no duplicate college Id stored here
    @JoinColumn(name = "user_id")
    private User user;



}
