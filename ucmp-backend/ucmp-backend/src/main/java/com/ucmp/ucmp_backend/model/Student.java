package com.ucmp.ucmp_backend.model;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import jakarta.persistence.*;
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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "college_id")
    private Profile profile;
}

