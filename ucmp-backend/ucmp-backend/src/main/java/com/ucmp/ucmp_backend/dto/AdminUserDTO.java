package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDTO {
    private String collegeId;
    private String name;
    private String email;
    private String role; // "ADMIN", "FACULTY", "STUDENT"
    private String department; // for faculty
    private String branch; // for student (batchName)
    private String sectionName; // for student
    private String year; // for student
    private String rollNumber; // for student
    private List<Long> sectionIds; // for faculty/student sections
}
