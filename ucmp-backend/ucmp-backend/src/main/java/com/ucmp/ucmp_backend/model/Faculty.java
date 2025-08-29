package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "faculties")
@Getter
@Setter
@NoArgsConstructor
public class Faculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String collegeId;

    private String department;
    private String designation;
    private String officeLocation;
    private String officeHours; // Could be a more complex type

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private  User user;

    @ManyToMany(mappedBy = "faculties") //if faculty teaches multiple batches
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
        this.user.setRole(Role.FACULTY);

        //initialize faculty specific fields
        this.department = department;
        this.officeLocation = officeLocation;
        this.designation = designation;
    }

//    @ManyToOne
//    @JoinColumn(name = "profile_id", nullable = false)
//    private Profile profile;
}
