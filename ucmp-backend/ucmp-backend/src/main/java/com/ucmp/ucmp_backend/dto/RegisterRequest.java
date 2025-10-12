package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.RoleName;
import lombok.*;
import jakarta.validation.constraints.*;

import java.util.List;

@Getter @Setter
@Data @Builder

@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank
    private String collegeId;
    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    @NotEmpty
    @NotEmpty
    private List<RoleName> roles = List.of(RoleName.STUDENT);
    private String rollNumber;
    private Integer year;
    private String branch;
    private String department;
    private String designation;


}