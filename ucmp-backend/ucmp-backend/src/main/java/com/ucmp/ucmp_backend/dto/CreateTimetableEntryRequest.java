package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request DTO for Admin to manually create a single timetable entry.
 * The validation engine runs BEFORE saving to detect conflicts.
 */
@Data
public class CreateTimetableEntryRequest {

    @NotNull(message = "Day is required")
    private DayOfWeek day;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Section ID is required")
    private Long sectionId;

    @NotNull(message = "Faculty ID is required")
    private Long facultyId;

    @NotBlank(message = "Academic term is required (e.g. 2026-27-ODD)")
    private String academicTerm;

    private EntryType entryType = EntryType.REGULAR;
}
