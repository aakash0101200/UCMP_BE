package com.ucmp.ucmp_backend.model;

/**
 * Lifecycle status of an AttendanceSession.
 *
 * ACTIVE    — Session is live; students can submit codes.
 * ENDED     — Faculty explicitly ended the session; manual-mark grace window may still be open.
 * CANCELLED — Session was voided before it started or mid-way (e.g., fire alarm).
 *             Does NOT count toward totalConducted in attendance calculation.
 */
public enum SessionStatus {
    ACTIVE,
    ENDED,
    CANCELLED
}
