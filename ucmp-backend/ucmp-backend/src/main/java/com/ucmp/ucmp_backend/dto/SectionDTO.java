package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {
    private Long id;
    private String sectionName;
    private Integer year;
    private Long batchId;

    public SectionDTO(Long id, String sectionName) {
        this.id = id;
        this.sectionName = sectionName;
    }
}