package com.ucmp.ucmp_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AttendanceStartRequestDto {
    private Long sectionId;
    private Long subjectId; // Which subject is being taught
    /**
     * IDs of additional sections to include (for MERGED sessions). Null = REGULAR.
     */
    private List<Long> mergedSectionIds;
    private Long scheduledFacultyId;
    private Double latitude;
    private Double longitude;
    private Double radiusInMeters;
    private Integer durationInMinutes;
}
