package com.ucmp.ucmp_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attendance_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── WHO is teaching ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_faculty_id", nullable = true)
    private Faculty scheduledFaculty;

    // ── PRIMARY section (always set; for MERGED sessions this is the faculty's OWN section)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // ── WHAT subject is being taught ───────────────────────────────────────────
    /**
     * Nullable for backward-compatibility with old sessions pre-dating this field.
     * Required for all new sessions — used for per-subject attendance calculation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = true)
    private Subject subject;

    // ── SESSION TYPE ───────────────────────────────────────────────────────────
    /**
     * REGULAR  — Standard single-section class.
     * MERGED   — One teacher covering 2+ sections at once (substitution or HOD-combined).
     *            All participating sections are in the attendanceSessionSections join table.
     * LAB      — Practical session.
     * MAKEUP   — Compensatory class for a previously cancelled slot.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionType sessionType = SessionType.REGULAR;

    // ── SESSION STATUS (lifecycle) ─────────────────────────────────────────────
    /**
     * ACTIVE    — Live; students can submit TOTP codes.
     * ENDED     — Faculty ended it; grace window may still be open for manual marks.
     * CANCELLED — Voided; does NOT count toward totalConducted.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    /**
     * Kept for backward-compatibility with existing queries.
     * isActive = (status == ACTIVE). Always keep in sync when updating status.
     */
    @Builder.Default
    private boolean isActive = true;

    // ── MERGED SECTIONS (only populated when sessionType = MERGED) ─────────────
    /**
     * All sections attending this session, including the primary.
     * For REGULAR sessions this list has exactly one entry (the primary section).
     * For MERGED sessions this has 2+ entries.
     * cascade = PERSIST so the join rows are saved automatically with the session.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceSessionSection> attendanceSessionSections = new ArrayList<>();

    // ── LOCATION GEOFENCE ──────────────────────────────────────────────────────
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Double radiusInMeters = 50.0;

    // ── TOTP ──────────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String secretSeed;

    // ── TIMING ────────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * How many minutes AFTER endTime faculty can still manually mark absent students.
     * 0 = no grace window after session ends.
     * Default: 15 minutes.
     */
    @Builder.Default
    private int manualMarkGraceMinutes = 15;

    @Builder.Default
    @Column(name = "duration_in_minutes")
    private Integer durationInMinutes = 40;

    // ── HELPERS ────────────────────────────────────────────────────────────────

    /**
     * Returns true if faculty manual marking is currently allowed.
     * Allowed during ACTIVE session OR within the grace window after ENDED.
     */
    public boolean isManualMarkAllowed() {
        if (status == SessionStatus.ACTIVE) return true;
        if (status == SessionStatus.ENDED && endTime != null) {
            return LocalDateTime.now().isBefore(endTime.plusMinutes(manualMarkGraceMinutes));
        }
        return false;
    }

    /**
     * Ends the session — sets both status and legacy isActive flag consistently.
     */
    public void endSession() {
        this.status   = SessionStatus.ENDED;
        this.isActive = false;
        this.endTime  = LocalDateTime.now();
    }

    /**
     * Cancels the session — voided, will not count in attendance denominator.
     */
    public void cancelSession() {
        this.status   = SessionStatus.CANCELLED;
        this.isActive = false;
        this.endTime  = LocalDateTime.now();
    }
}
