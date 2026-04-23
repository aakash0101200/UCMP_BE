package com.ucmp.ucmp_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreateStudentRequest {
    @NotBlank
    private String collegeId;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String rollNumber;

    @NotNull
    private Integer year;

    // The crucial mapping: assigns the student to a specific batch and section
    @NotNull
    private Long batchId;

    @NotNull
    private Long sectionId;
}