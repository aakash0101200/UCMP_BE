package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyAvailabilityResponse {
    private Long facultyId;
    private String facultyName;
    private String collegeId;
    private String department;
    private String designation;
    private String status;              // FREE or BUSY
    private String conflictDescription; // If busy, description of conflict
    private String expertiseRank;       // Direct Expertise, Departmental Match, General Backup
    private long weeklyWorkloadSlots;   // Total slots scheduled for term
    private long recentSubstitutionCount; // Total substitution overrides covered on the date
}
