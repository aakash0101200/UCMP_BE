package com.ucmp.ucmp_backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ClassCancellationRequestDto {
    private LocalDate cancellationDate;
    private String reason;
}
