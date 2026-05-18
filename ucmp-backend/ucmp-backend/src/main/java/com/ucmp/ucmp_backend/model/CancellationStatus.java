package com.ucmp.ucmp_backend.model;

/**
 * Approval workflow status for a ClassCancellation.
 *
 * AUTO_APPROVED     — Default: faculty can cancel without HOD sign-off.
 *                     Suitable for most colleges with trusted faculty.
 * PENDING_APPROVAL  — HOD must review before the cancellation is confirmed to students.
 *                     Activated when college policy requires HOD oversight.
 * APPROVED          — HOD explicitly approved the cancellation.
 * REJECTED          — HOD rejected; class is still ON. Students are notified.
 */
public enum CancellationStatus {
    AUTO_APPROVED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
}
