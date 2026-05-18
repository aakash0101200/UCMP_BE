package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(
    name = "attendance_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uk_student_session",
            columnNames = {"student_id", "session_id"}
        )
    }
)
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── WHO attended ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ── WHICH session ─────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession attendanceSession;

    // ── WHEN ──────────────────────────────────────────────────────────────────
    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @PrePersist
    protected void onCreate() {
        if (this.markedAt == null) this.markedAt = LocalDateTime.now();
    }

    // ── LOCATION (student's position at mark time) ────────────────────────────
    private Double markedLatitude;
    private Double markedLongitude;

    // ── AUDIT: HOW was this record created? ────────────────────────────────────
    /**
     * STUDENT_TOTP   — Student self-marked with a valid rotating code (+ location if required).
     * FACULTY_MANUAL — Faculty manually marked the student present (dead battery, no internet,
     *                  latecomer physically verified) within the grace window.
     * SYSTEM         — Reserved for future automated marking (NFC, BLE, etc.).
     *
     * This field is critical for HOD audit reports. A high FACULTY_MANUAL ratio
     * for a specific faculty should trigger a review.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "marked_by", nullable = false, length = 20)
    @Builder.Default
    private MarkSource markedBy = MarkSource.STUDENT_TOTP;

    /**
     * Only populated when markedBy = FACULTY_MANUAL.
     * Records which faculty member manually overrode the absent status.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_faculty_id", nullable = true)
    private Faculty markedByFaculty;

    /**
     * Only populated when markedBy = FACULTY_GRACE.
     * Records the official reason for waiving the absence.
     */
    @Column(name = "grace_reason", length = 255)
    private String graceReason;
}
