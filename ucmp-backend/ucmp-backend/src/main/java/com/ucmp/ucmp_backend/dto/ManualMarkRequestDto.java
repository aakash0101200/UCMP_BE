package com.ucmp.ucmp_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ManualMarkRequestDto {
    private List<String> studentCollegeIds;  // e.g. ["22BCS001", "22BCS015"]
    private String reason;                    // e.g. "Student phones were dead"
}
