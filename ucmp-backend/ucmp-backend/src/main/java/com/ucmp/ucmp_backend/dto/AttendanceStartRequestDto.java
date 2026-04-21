package com.ucmp.ucmp_backend.dto;

import lombok.Data;

@Data
public class AttendanceStartRequestDto {
    private Long sectionId;
    private Double latitude;
    private Double longitude;
    private Double radiusInMeters;
}
