package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "timetable_overrides",
    indexes = {
        @Index(name = "idx_override_date", columnList = "override_date"),
        @Index(name = "idx_override_new_fac", columnList = "new_faculty_id, override_date"),
        @Index(name = "idx_override_orig_fac", columnList = "original_faculty_id, override_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_entry_id", nullable = true)
    private TimetableEntry timetableEntry;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 20)
    private OverrideType overrideType;

    // --- FACULTY OVERRIDES ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_faculty_id", nullable = true)
    private Faculty originalFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_faculty_id", nullable = true)
    private Faculty newFaculty;

    // --- ROOM OVERRIDES ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_room_id", nullable = true)
    private Room originalRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_room_id", nullable = true)
    private Room newRoom;

    // --- TIME OVERRIDES ---
    @Column(name = "original_start_time")
    private LocalTime originalStartTime;

    @Column(name = "new_start_time")
    private LocalTime newStartTime;

    @Column(name = "original_end_time")
    private LocalTime originalEndTime;

    @Column(name = "new_end_time")
    private LocalTime newEndTime;

    // --- SUBJECT OVERRIDE (for extra classes or changes) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = true)
    private Subject subject;

    // --- SECTIONS INVOLVED (supports merged classes & single section) ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "timetable_override_sections",
        joinColumns = @JoinColumn(name = "override_id"),
        inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    @Builder.Default
    private List<Section> sections = new ArrayList<>();

    @Column(name = "effective_from", nullable = true)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = true)
    private LocalDate effectiveTo;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean isRecurring = false;

    @Column(name = "recurring_pattern", length = 100, nullable = true)
    private String recurringPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OverrideStatus status = OverrideStatus.ACTIVE;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
