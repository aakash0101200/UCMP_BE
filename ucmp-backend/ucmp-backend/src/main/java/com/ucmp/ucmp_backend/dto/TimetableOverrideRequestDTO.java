package com.ucmp.ucmp_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TimetableOverrideRequestDTO {
    private Long timetableEntryId;
    private String overrideDate; // YYYY-MM-DD
    private String overrideType; // CANCELLED, SUBSTITUTE, EXTRA_CLASS, ROOM_CHANGE, TIME_CHANGE, MERGED_CLASS
    private Long newFacultyId;
    private Long newRoomId;
    private String newStartTime; // HH:mm
    private String newEndTime;   // HH:mm
    private Long subjectId;
    private List<Long> sectionIds; // Target sections
    private String reason;
    private String status; // PENDING, CONFIRMED, ACTIVE, etc.
    private String academicTerm; // "2026-27-ODD"
    private String effectiveFrom; // YYYY-MM-DD
    private String effectiveTo;   // YYYY-MM-DD
    private Boolean isRecurring;
    private String recurringPattern; // "DAILY", "WEEKLY"
}
