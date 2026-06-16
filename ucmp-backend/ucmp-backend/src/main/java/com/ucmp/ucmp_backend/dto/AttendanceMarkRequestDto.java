package com.ucmp.ucmp_backend.dto;

import lombok.Data;

@Data
public class AttendanceMarkRequestDto {
    private Long sessionId;
    private String code;
    private Double latitude;
    private Double longitude;
    private Double accuracy; // GPS accuracy in meters — used for smart radius validation
    private String deviceFingerprint;
}
