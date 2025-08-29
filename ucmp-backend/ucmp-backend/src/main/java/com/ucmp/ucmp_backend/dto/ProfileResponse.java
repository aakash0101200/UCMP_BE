package com.ucmp.ucmp_backend.dto;

import com.ucmp.ucmp_backend.model.Role;
import jakarta.validation.constraints.Email;
import jdk.jfr.Name;
import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private String collegeId;
    private String email;
    private String role;
    private String name;
    private String profilePictureUrl;


}
