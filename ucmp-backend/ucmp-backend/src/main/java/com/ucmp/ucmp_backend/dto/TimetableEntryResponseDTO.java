package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.EntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Response DTO for a TimetableEntry — flattened for easy frontend consumption.
 * The frontend weekly grid only needs names, not nested objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryResponseDTO {

    private Long id;

    // WHEN
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;

    // WHAT
    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    // WHERE
    private Long roomId;
    private String roomName;
    private String roomBuilding;

    // WHO attends
    private Long sectionId;
    private String sectionName;

    // WHO teaches
    private Long facultyId;
    private String facultyName;

    // META
    private String academicTerm;
    private EntryType entryType;
}
