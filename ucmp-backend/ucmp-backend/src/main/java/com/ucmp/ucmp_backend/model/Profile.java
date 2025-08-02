package com.ucmp.ucmp_backend.model;

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
    @Column(name = "college_id")
    private String collegeId;

    private String name;

    @Email
    private String email;

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Student student;


}
