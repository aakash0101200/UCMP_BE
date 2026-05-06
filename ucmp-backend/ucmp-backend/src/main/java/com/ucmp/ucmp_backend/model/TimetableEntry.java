package com.ucmp.ucmp_backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
    name = "timetable_entries",
    indexes = {
        @Index(name = "idx_tt_section_day", columnList = "section_id, day, start_time"),
        @Index(name = "idx_tt_faculty_day", columnList = "faculty_id, day, start_time"),
        @Index(name = "idx_tt_room_day",    columnList = "room_id, day, start_time")
    },
    uniqueConstraints = {
        // Hard constraint #1: No section can have 2 classes at the same time
        @UniqueConstraint(name = "uk_section_day_slot", columnNames = {"section_id", "day", "start_time", "academic_term"}),
        // Hard constraint #2: No faculty can teach 2 classes at the same time
        @UniqueConstraint(name = "uk_faculty_day_slot", columnNames = {"faculty_id", "day", "start_time", "academic_term"}),
        // Hard constraint #3: No room can host 2 classes at the same time
        @UniqueConstraint(name = "uk_room_day_slot",    columnNames = {"room_id",    "day", "start_time", "academic_term"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- WHEN ---
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false, length = 10)
    private DayOfWeek day;

    @NotNull
    @Column(name = "start_time", nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // --- WHAT ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // --- WHERE ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // --- WHO attends ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // --- WHO teaches ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    // --- SEMESTER ---
    @NotBlank
    @Column(name = "academic_term", nullable = false)
    private String academicTerm;   // e.g. "2026-27-ODD"

    // --- CLASS TYPE ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EntryType entryType = EntryType.REGULAR;
}