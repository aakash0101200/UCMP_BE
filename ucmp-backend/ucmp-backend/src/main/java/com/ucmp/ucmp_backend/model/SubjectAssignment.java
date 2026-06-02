package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * SubjectAssignment links a Subject → Faculty → Section for a given academic term.
 * This is the INPUT to the auto-generator:
 * "Prof. Sharma teaches Data Structures to Section A in 2026-27-ODD, 4 slots/week"
 *
 * The auto-generator converts these assignments into actual TimetableEntry records.
 */
@Entity
@Table(
    name = "subject_assignments",
    uniqueConstraints = {
        // A subject can only be assigned to a section once per term (one primary faculty member)
        @UniqueConstraint(
            name = "uk_assignment_subject_section_term",
            columnNames = {"subject_id", "section_id", "academic_term"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    /**
     * Academic term identifier, e.g. "2026-27-ODD", "2026-27-EVEN"
     * Used to separate timetables across semesters.
     */
    @NotBlank
    @Column(name = "academic_term", nullable = false)
    private String academicTerm;

    /**
     * How many timetable slots per week this assignment should occupy.
     * e.g. 4 for a 4-lecture/week subject, 2 for a 2-hour lab/week.
     * The auto-generator uses this to decide how many TimetableEntry records to create.
     */
    @Min(1)
    @Column(nullable = false)
    private int weeklySlots;

    /**
     * Optional URL to the Google Classroom for this subject-section.
     * Set by the owning faculty via PATCH /api/timetable/assignment/{id}/classroom-link.
     * Null means the faculty has not configured it yet.
     */
    @Column(name = "google_classroom_link", length = 1024)
    private String googleClassroomLink;
}
