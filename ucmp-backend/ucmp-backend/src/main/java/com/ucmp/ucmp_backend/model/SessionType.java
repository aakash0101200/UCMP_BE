package com.ucmp.ucmp_backend.model;

/**
 * Classifies what kind of class session is being run.
 *
 * REGULAR  — Standard single-section class (the default).
 * MERGED   — One teacher covering 2+ sections simultaneously in the same room.
 *            Covers two scenarios:
 *            (a) Teacher's own section + a substitute slot for another section.
 *            (b) Two sections merged by HOD for a combined lecture.
 *            All merged sections are tracked in AttendanceSessionSection join table.
 * LAB      — Practical/lab session (may have different attendance rules).
 * MAKEUP   — A compensatory class for a previously cancelled slot.
 */
public enum SessionType {
    REGULAR,
    MERGED,
    LAB,
    MAKEUP
}
