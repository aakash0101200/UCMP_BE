package com.ucmp.ucmp_backend.model;

import com.ucmp.ucmp_backend.service.AnnouncementService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "batches")
@Getter
@Setter
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchName;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private Set<Student> students = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "batch_faculty",
            joinColumns = @JoinColumn(name = "batch_id"),
            inverseJoinColumns = @JoinColumn(name = "faculty_id")
            )
    private Set<Faculty> faculties = new HashSet<>();

    public void addFaculty(Faculty faculty) {
        faculties.add(faculty);
    }

}
