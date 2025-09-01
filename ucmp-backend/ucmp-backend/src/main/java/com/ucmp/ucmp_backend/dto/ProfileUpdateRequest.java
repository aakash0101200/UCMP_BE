package com.ucmp.ucmp_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    private String name; //editable user fields

    //Editable Profile fields
    private String profilePictureUrl;

    private String phoneNumber;
    private String address;
    private String ___;

}
