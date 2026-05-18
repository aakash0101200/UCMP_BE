package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Join table linking an AttendanceSession to one or more Sections.
 *
 * For REGULAR sessions: exactly one row (the primary section).
 * For MERGED sessions:  one row per section — including the primary.
 *
 * This allows a single rotating TOTP code to serve students from multiple sections
 * when a teacher covers two sections simultaneously (HOD-combined lecture, substitution, etc.)
 */
@Entity
@Table(
    name = "attendance_session_sections",
    uniqueConstraints = @UniqueConstraint(
        name  = "uk_session_section",
        columnNames = {"session_id", "section_id"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceSessionSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    /**
     * True for the teacher's own / primary section.
     * False for merged-in sections (substitution or combined lecture).
     * Useful for display — e.g., showing "Substituting Section B" in faculty UI.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isPrimary = false;
}
