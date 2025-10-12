package com.ucmp.ucmp_backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    @Size(min = 2, max = 100)
    private String name; //editable user fields

    //Editable Profile fields
    @Size(max = 500)
    private String profilePictureUrl;

    @Pattern(
            regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10}$",
            message = "Invalid phone number"
    )
    private String phoneNumber;

    @Size(min = 2, max = 200)
    private String address;

}
