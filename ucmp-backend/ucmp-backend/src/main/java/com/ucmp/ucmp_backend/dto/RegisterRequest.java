package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.Role;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.*;

//@Data
//public class RegisterRequest {
//     private String name;
//     @Email private String email;
//     @Size(min = 8) private String password;
//
//    private Role role;
//     private String collegeId; // STU101
//
//
//
//}
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Data @Builder
public class RegisterRequest {
    private String collegeId;
    private String name;
//    private String lastName;
    private String email;
    private String password;
    private Role role; // "STUDENT", "FACULTY", "ADMIN"

    private String branch;
    private String year;
    private String rollNumber;

    private String department;
    public String Designation;

}

