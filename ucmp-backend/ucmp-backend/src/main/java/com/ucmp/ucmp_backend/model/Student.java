package com.ucmp.ucmp_backend.model;
import com.ucmp.ucmp_backend.repository.StudentRepository;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(name = "students")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Student {



    @Id
    @Column(name = "college_id")
    private String collegeId;

    @OneToOne(mappedBy = "student",cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId
    @JoinColumn()
    private Profile profile;
}

