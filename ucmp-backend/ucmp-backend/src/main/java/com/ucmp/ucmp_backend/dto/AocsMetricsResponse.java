package com.ucmp.ucmp_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AocsMetricsResponse {
    private long activeOverridesCount;
    private long pendingReplacementsCount;
    private double facultyUtilizationRate;
    private long cancelledClassesCount;
    private long liveOngoingLecturesCount;
}
