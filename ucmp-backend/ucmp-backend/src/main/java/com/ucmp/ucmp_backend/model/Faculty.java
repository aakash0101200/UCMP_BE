package com.ucmp.ucmp_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "faculties",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Same as user.ID (MapsId)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private  User user;

    private String collegeId;
    private String department;
    private String designation;
    private String officeLocation;
    private String officeHours; // Could be a more complex type



    @ManyToMany
    @JoinTable(
            name = "section_faculty",
            joinColumns = @JoinColumn(name = "faculty_id"),
            inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private Set<Section> sections = new HashSet<>();

//    @OneToMany(mappedBy = "faculty", cascade =  CascadeType.ALL, orphanRemoval = true)
//    private Set<Schedule> schedules = new HashSet<>();

//    // One faculty member can advise many students
//    @OneToMany(mappedBy = "facultyAdvisor", fetch = FetchType.LAZY)
//    private List<Student> advisedStudents = new ArrayList<>();

    public Faculty(String collegeId, String password, String name, String designation, String email, String department, String officeLocation) {
        this.user = new User();
        this.user.setCollegeId(collegeId);
        this.user.setPassword (password);
        this.user.setName(name);
        this.user.setEmail(email);
//        this.user.setRole(Role.FACULTY);

        //initialize faculty specific fields
        this.department = department;
        this.officeLocation = officeLocation;
        this.designation = designation;
    }


//    @ManyToOne
//    @JoinColumn(name = "profile_id", nullable = false)
//    private Profile profile;
}
