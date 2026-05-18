package com.ucmp.ucmp_backend.model;

/**
 * How an AttendanceRecord was created — the audit source.
 *
 * STUDENT_TOTP   — Student self-marked by submitting a valid rotating TOTP code.
 *                  Location was also validated if session required it.
 * FACULTY_MANUAL — Faculty overrode the absent status within the grace window.
 *                  Used for: dead battery, no internet, latecomer physically verified.
 *                  The markedByFaculty field will be populated.
 * FACULTY_GRACE  — Faculty officially waived the absence outside of normal attendance.
 *                  Used for: medical emergencies, official college duty, etc.
 *                  No time window restriction. Requires a grace reason.
 * SYSTEM         — Reserved for future automated marking (e.g., NFC/BLE auto-check-in).
 */
public enum MarkSource {
    STUDENT_TOTP,
    FACULTY_MANUAL,
    FACULTY_GRACE,
    SYSTEM
}
