package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Role;
import jakarta.validation.constraints.Email;
import jdk.jfr.Name;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private String collegeId;
    private String email;
    private String name;


    //_______________
    private String profilePictureUrl;
    private String phoneNumber;
    private String address;
    //'''

    private List<String> roles;

    //role specific subobjects (nullable)
    private StudentInfo student;
    private FacultyInfo faculty;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentInfo {
        private String rollNumber;
        private String year;
        private String batchName;
        private String sectionName;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FacultyInfo {
        private String department;
        private String __;
        private String ___;
        private String ____;

    }



}
