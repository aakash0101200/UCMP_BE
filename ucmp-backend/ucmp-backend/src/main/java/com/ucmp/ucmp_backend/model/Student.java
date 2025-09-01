package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    private Long id; //same as user.ID (MapsId)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;   //link back to user

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY) // error expected )(Section implies batch if modeled properly
    @JoinColumn(name = "section_id")
    private Section section;

    @NotNull
    @Column(unique = true)
    private String rollNumber;

    private String CollegeId;

    //private String year; //consider making year an enum or separate table if needed


}