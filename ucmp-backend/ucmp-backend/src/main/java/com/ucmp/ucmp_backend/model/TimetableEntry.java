//package com.ucmp.ucmp_backend.model;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.*;
//
//import java.time.DayOfWeek;
//import java.time.LocalTime;
//
//@Entity
//@Table(
//        name = "timetable_entries",
//        indexes = {
//                @Index(name = "idx_tt_section_day", columnList = "section_id, day, start_time"),
//                @Index(name = "idx_tt_faculty_day", columnList = "faculty_id, day, start_time")
//        },
//        uniqueConstraints = {
//                @UniqueConstraint(
//                        name = "uk_tt_section_slot",
//                        columnNames = {"section_id", "day", "start_time", "subject"}
//                )
//        }
//)
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class TimetableEntry {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // Use java.time.DayOfWeek
//    @Enumerated(EnumType.STRING)
//    @Column(name = "day", nullable = false, length = 10)
//    private DayOfWeek day;
//
//    @NotNull
//    @Column(name = "start_time", nullable = false)
//    @JsonFormat(pattern = "HH:mm")
//    private LocalTime startTime;
//
//    @NotNull
//    @Column(name = "end_time", nullable = false)
//    @JsonFormat(pattern = "HH:mm")
//    private LocalTime endTime;
//
//    @NotBlank
//    @Column(name = "subject", nullable = false, length = 120)
//    private String subject;
//
//    @Column(name = "location", length = 120)
//    private String location;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "section_id", nullable = false)
//    private Section section;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "faculty_id", nullable = false)
//    private Faculty faculty;
//}