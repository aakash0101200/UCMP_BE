package com.ucmp.ucmp_backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse { 
    private String token;
    private ProfileResponse profile;
}
