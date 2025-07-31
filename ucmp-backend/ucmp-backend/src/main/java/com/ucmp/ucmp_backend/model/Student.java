package com.ucmp.ucmp_backend.model;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@NoArgsConstructor @AllArgsConstructor
@Data
public class Student {

    @Id
    private String collegeId;

    @OneToOne
    @JoinColumn(name = "college_id")
    @MapsId
    private Profile profile;


}
