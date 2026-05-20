package com.ucmp.ucmp_backend.dto.websocket;

import java.time.LocalDateTime;

public record RosterUpdateEvent(
    Long sessionId,
    Long studentId,
    String studentName,
    String collegeId,
    LocalDateTime markedAt,
    String markSource
) {}
