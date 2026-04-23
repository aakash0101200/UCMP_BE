package com.ucmp.ucmp_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name= "sections")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sectionName;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @OneToMany(mappedBy = "section", cascade =  CascadeType.ALL, orphanRemoval = true)
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

//    @OneToMany(mappedBy = "section", cascade =  CascadeType.ALL, orphanRemoval = true)
//    private Set<Schedule> schedules = new HashSet<>();

    //getter & setter
    public Long getId() { return id;}
    public void setId(Long id) { this.id = id;}
    public String getSectionName() { return sectionName;}
    public void setSectionName(String sectionName) { this.sectionName = sectionName;}
    public Batch getBatch() { return batch;}
    public  void setBatch(Batch batch) { this.batch = batch;}
    public Set<Student> getStudents() { return students;}
    public  void setStudents(Set<Student> students) { this.students = students;}
    public Set<Faculty> getFaculties(){return faculties;}
    public void setFaculties(Set<Faculty> faculties) { this.faculties = faculties;}

}
