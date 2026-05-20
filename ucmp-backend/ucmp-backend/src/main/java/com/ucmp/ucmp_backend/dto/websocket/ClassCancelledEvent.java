package com.ucmp.ucmp_backend.dto.websocket;

import java.time.LocalDate;

public record ClassCancelledEvent(
    Long timetableEntryId,
    String subjectName,
    String subjectCode,
    LocalDate cancellationDate,
    String scheduledTime, // e.g. "10:00 - 11:00"
    String reason
) {}
