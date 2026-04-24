package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StudentAttendanceDTO {
    private String name;
    private String collegeId;
    private LocalDateTime markedAt;
}