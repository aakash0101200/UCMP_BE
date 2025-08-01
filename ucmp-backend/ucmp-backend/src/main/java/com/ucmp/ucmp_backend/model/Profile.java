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
    private String collegeId;

    private String name;

    @Email
    private String email;


}
