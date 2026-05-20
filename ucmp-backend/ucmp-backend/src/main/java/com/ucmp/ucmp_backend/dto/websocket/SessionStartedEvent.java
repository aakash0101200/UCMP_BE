package com.ucmp.ucmp_backend.dto.websocket;

import java.time.LocalDateTime;

public record SessionStartedEvent(
    Long sessionId,
    String subjectName,
    String subjectCode,
    String sectionName,
    String sessionType,
    LocalDateTime startTime
) {}
