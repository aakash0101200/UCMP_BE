package com.ucmp.ucmp_backend.dto;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.List;
import jakarta.validation.constraints.Size;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private String collegeId;
    private String email;

    private String name;

    // Profile fields
    private String profilePictureUrl;
    private String phoneNumber;
    private String address;

    private List<String> roles;

    // Role-specific subobjects (nullable)
    private StudentInfo student;
    private FacultyInfo faculty;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentInfo {
        private String rollNumber;
        private String year;
        private String batchName;
        private String sectionName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FacultyInfo {
        private String department;
        private String designation;
        private String officeLocation;
        private String officeHours;
        private String status; // ACTIVE, INACTIVE, etc.
    }

}
