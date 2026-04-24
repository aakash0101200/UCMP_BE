package com.ucmp.ucmp_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private User user;   //link back to user

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="batch_id")
    private Batch batch;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) // error expected (Section implies batch if modeled properly
    @JoinColumn(name = "section_id")
    private Section section;

    @NotBlank
    @Column(unique = true)
    private String rollNumber;

    @Column(name = "name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "college_id", unique = true, nullable = false)
    private String collegeId;



    private String year; //consider making year an enum or separate table if needed


}