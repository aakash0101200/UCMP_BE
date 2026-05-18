package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAssignmentDTO {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long facultyId;
    private String facultyName;
    private String facultyCollegeId;
    private Long sectionId;
    private String sectionName;
    private String academicTerm;
    private int weeklySlots;
}
