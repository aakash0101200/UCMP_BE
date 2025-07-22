package com.ucmp.ucmp_backend.model;

import lombok.Data;
import lombok.Getter;

@Data
public class LoginRequest {
    private String collegeId;  // College ID Match the frontend's login form of Frontend
    private String password;

    private String role;

    public String getId() {
        return collegeId;
    }
}
// No-arg constructor is provided by @Data annotation
