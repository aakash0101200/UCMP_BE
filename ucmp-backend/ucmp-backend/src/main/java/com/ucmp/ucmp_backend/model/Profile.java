package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "profiles")
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
@Data
public class Profile {

    @Id
    @Column (nullable = false, unique = true)
    private String collegeId;

    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;


}
