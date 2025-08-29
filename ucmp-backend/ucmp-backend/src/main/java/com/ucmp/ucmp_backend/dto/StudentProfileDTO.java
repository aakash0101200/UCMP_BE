package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class StudentProfileDTO {
    private String collegeId;

    private Long profileId;
    private String name;
    private String email;

    private Long section;
}
