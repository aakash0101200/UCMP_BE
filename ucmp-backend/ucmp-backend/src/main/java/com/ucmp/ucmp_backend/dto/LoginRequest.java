package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.Role;
import lombok.Data;

//@Data
//public class LoginRequest {
//    private String email;
//    private String password;
//}

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    private String collegeId;
    private String password;
    private Role role;
}
