package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceSummaryDTO {
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private int totalClasses;   // total sessions held for this subject
    private int attended;       // sessions this student attended
    private double percentage;  // (attended / totalClasses) * 100
    private String status;      // "SAFE" | "WARNING" | "DANGER"
}
