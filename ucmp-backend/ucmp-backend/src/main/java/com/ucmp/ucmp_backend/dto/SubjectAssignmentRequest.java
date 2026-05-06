package com.ucmp.ucmp_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO to create a SubjectAssignment:
 * "Prof. Sharma (facultyId) teaches Data Structures (subjectId)
 *  to Section A (sectionId) in 2026-27-ODD, 4 slots/week"
 *
 * This is the INPUT that the auto-generator (Phase 6) consumes to
 * produce actual timetable entries.
 */
@Data
public class SubjectAssignmentRequest {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Faculty ID is required")
    private Long facultyId;

    @NotNull(message = "Section ID is required")
    private Long sectionId;

    @NotBlank(message = "Academic term is required (e.g. 2026-27-ODD)")
    private String academicTerm;

    @Min(value = 1, message = "Weekly slots must be at least 1")
    private int weeklySlots;
}
