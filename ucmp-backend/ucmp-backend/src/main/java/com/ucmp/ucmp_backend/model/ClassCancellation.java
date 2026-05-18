package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records a one-day cancellation for a recurring TimetableEntry slot.
 *
 * Design principle:
 * - The TimetableEntry is NEVER deleted or modified.
 * - This table records EXCEPTIONS — "on date X, slot Y did not happen."
 * - Attendance calculation is unaffected (cancelled sessions were never started,
 *   so they never appear in AttendanceSession — the denominator is naturally correct).
 * - This table is used ONLY for schedule display (showing the red "Cancelled" banner)
 *   and for term-level reporting ("Section A had 3 cancellations in CS301 this semester").
 */
@Entity
@Table(
    name = "class_cancellations",
    uniqueConstraints = @UniqueConstraint(
        name        = "uk_entry_date",
        columnNames = {"timetable_entry_id", "cancellation_date"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── WHICH recurring slot was cancelled ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private TimetableEntry timetableEntry;

    // ── WHICH specific date ────────────────────────────────────────────────────
    @Column(name = "cancellation_date", nullable = false)
    private LocalDate cancellationDate;

    // ── WHO cancelled it ──────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_faculty_id", nullable = false)
    private Faculty cancelledBy;

    // ── WHY ───────────────────────────────────────────────────────────────────
    @Column(length = 255)
    private String reason;     // e.g. "Faculty on duty", "National holiday", "Power outage"

    // ── APPROVAL WORKFLOW ─────────────────────────────────────────────────────
    /**
     * AUTO_APPROVED  — Default. Students see "Cancelled" immediately.
     * PENDING_APPROVAL — HOD must review (for stricter colleges).
     * APPROVED       — HOD confirmed the cancellation.
     * REJECTED       — HOD rejected; class is still ON; students notified.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CancellationStatus approvalStatus = CancellationStatus.AUTO_APPROVED;

    // ── HOD approval (optional) ───────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id", nullable = true)
    private Faculty reviewedBy;

    @Column
    private LocalDateTime reviewedAt;

    // ── AUDIT ─────────────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Whether this cancellation is currently effective:
     * - AUTO_APPROVED or APPROVED → yes.
     * - REJECTED or PENDING_APPROVAL → no.
     */
    public boolean isEffective() {
        return approvalStatus == CancellationStatus.AUTO_APPROVED
            || approvalStatus == CancellationStatus.APPROVED;
    }
}
