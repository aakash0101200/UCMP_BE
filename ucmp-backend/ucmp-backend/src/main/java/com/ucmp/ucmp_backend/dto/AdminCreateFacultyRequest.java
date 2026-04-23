package com.ucmp.ucmp_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class AdminCreateFacultyRequest {
    @NotBlank
    private String collegeId;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String department;

    @NotBlank
    private String designation;

    // The crucial mapping: assigns sections during account creation
    private List<Long> sectionIds;
}