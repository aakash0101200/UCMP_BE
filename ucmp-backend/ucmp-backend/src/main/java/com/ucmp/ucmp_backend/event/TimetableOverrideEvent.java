package com.ucmp.ucmp_backend.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TimetableOverrideEvent {
    private final Long overrideId;
    private final String overrideType;
    private final LocalDate date;
    private final List<Long> sectionIds;
    private final Long newFacultyId;
    private final Long originalFacultyId;
    private final String reason;
}
