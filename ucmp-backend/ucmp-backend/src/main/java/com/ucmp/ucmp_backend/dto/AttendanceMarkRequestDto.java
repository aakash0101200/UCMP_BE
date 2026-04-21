package com.ucmp.ucmp_backend.dto;

import lombok.Data;

@Data
public class AttendanceMarkRequestDto {
    private Long sessionId;
    private String code;
    private Double latitude;
    private Double longitude;
}
