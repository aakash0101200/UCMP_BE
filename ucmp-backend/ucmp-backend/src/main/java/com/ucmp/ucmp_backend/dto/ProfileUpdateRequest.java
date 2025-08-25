package com.ucmp.ucmp_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String name;
    private String profilePictureUrl;

    //Student fields
    private String department;
    private String year;
    private String rollNumber;

    //Faculty fields
    private String designation;
    private String officeLocation;
    private String officeHours;

}
