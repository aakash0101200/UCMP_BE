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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String collegeId;

    private String rollNumber;
    private String department;
    private String year;


//    @OneToOne(mappedBy = "student",cascade = CascadeType.ALL, orphanRemoval = true)
//    @MapsId
//    @JoinColumn()
//    private Profile profile;
    //link to user
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name="batch_id")
    private Batch batch;

    @ManyToOne // error expected
    @JoinColumn(name = "section_id")
    private Section section;



//    @ManyToOne
//    @JoinColumn(name = "profile_id", nullable = false)
//    private Profile profile;
    //profile will be accessed through user .getprofile
}

