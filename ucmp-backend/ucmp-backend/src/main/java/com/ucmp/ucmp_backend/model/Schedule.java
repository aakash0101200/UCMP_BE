//package com.ucmp.ucmp_backend.model;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//
//@Entity
//@Table(name = "schedules")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Schedule {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String dayOfWeek;
//    private LocalDateTime startTime;
//    private LocalTime endTime;
//    private String location;
//    //subject can be a alag entity too...................................
//
//    //Each schedule belongs to one section
//    @ManyToOne (fetch = FetchType.LAZY)
//    @JoinColumn(name = "section_id", nullable = false)
//    private Section section;
//
//    //Each schedule is taught by one faculty
//    @ManyToOne (fetch = FetchType.LAZY)
//    @JoinColumn(name = "faculty_id", nullable = false)
//    private Faculty faculty;
//
//    //constructor
//
//
//}
