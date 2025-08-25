package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name= "sections")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sectionName;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @OneToMany(mappedBy = "section")
    private Set<Student> students = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "section_faculty",
            joinColumns = @JoinColumn(name =
            "section_id"),
            inverseJoinColumns = @JoinColumn(name =
            "faculty_id")
    )
    private Set<Faculty> faculties = new HashSet<>();


    public Set<Faculty> getFaculties() {
        return faculties;
    }
}
