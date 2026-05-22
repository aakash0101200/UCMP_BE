package com.ucmp.ucmp_backend.validator;

import com.ucmp.ucmp_backend.dto.TimetableEntryResponseDTO;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Room;
import com.ucmp.ucmp_backend.model.Section;
import com.ucmp.ucmp_backend.model.TimetableOverride;
import com.ucmp.ucmp_backend.service.TimetableResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TimetableOverrideValidator {

    private final TimetableResolutionService resolutionService;
    private final com.ucmp.ucmp_backend.repository.TimetableOverrideRepository overrideRepo;

    /**
     * Validates a candidate override for scheduling conflicts (Faculty, Room, Section).
     */
    public void validate(TimetableOverride candidate, String term) {
        if (candidate.getTimetableEntry() != null) {
            List<TimetableOverride> existingOverrides = overrideRepo.findActiveOverridesByEntryAndDate(
                    candidate.getTimetableEntry().getId(), candidate.getOverrideDate());
            if (candidate.getId() != null) {
                existingOverrides = existingOverrides.stream()
                        .filter(o -> !o.getId().equals(candidate.getId()))
                        .collect(java.util.stream.Collectors.toList());
            }

            // 1. Cancelled slot receiving substitution/override check
            boolean isCancelled = existingOverrides.stream()
                    .anyMatch(o -> o.getOverrideType() == com.ucmp.ucmp_backend.model.OverrideType.CANCELLED);
            if (isCancelled && candidate.getOverrideType() != com.ucmp.ucmp_backend.model.OverrideType.CANCELLED) {
                throw new IllegalStateException("Cannot apply override to a class slot that is cancelled.");
            }

            // 2. Duplicate override type check (e.g. duplicate substitutions)
            if (candidate.getOverrideType() != com.ucmp.ucmp_backend.model.OverrideType.CANCELLED) {
                boolean duplicateType = existingOverrides.stream()
                        .anyMatch(o -> o.getOverrideType() == candidate.getOverrideType());
                if (duplicateType) {
                    throw new IllegalStateException("An override of type " + candidate.getOverrideType() + " already exists for this slot on this date.");
                }
            }
        }

        LocalTime start = candidate.getNewStartTime() != null ? candidate.getNewStartTime() : 
                (candidate.getTimetableEntry() != null ? candidate.getTimetableEntry().getStartTime() : null);
        LocalTime end = candidate.getNewEndTime() != null ? candidate.getNewEndTime() : 
                (candidate.getTimetableEntry() != null ? candidate.getTimetableEntry().getEndTime() : null);

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and End time must be specified or resolvable from a template.");
        }

        if (start.isAfter(end) || start.equals(end)) {
            throw new IllegalArgumentException("Start time must be before End time.");
        }

        // 1. Validate Faculty Conflict (if a new faculty is assigned to teach)
        Faculty targetFaculty = candidate.getNewFaculty() != null ? candidate.getNewFaculty() : 
                (candidate.getTimetableEntry() != null ? candidate.getTimetableEntry().getFaculty() : null);

        if (targetFaculty != null && candidate.getOverrideType() != com.ucmp.ucmp_backend.model.OverrideType.CANCELLED) {
            List<TimetableEntryResponseDTO> facultySchedule = resolutionService.getResolvedScheduleForFaculty(
                    targetFaculty.getId(), candidate.getOverrideDate(), term);

            for (TimetableEntryResponseDTO slot : facultySchedule) {
                // If it is the same override (updating), or same template entry being replaced, skip
                if (candidate.getId() != null && slot.getOverrideId() != null && slot.getOverrideId().equals(candidate.getId())) {
                    continue;
                }
                if (candidate.getTimetableEntry() != null && slot.getId() != null && slot.getId().equals(candidate.getTimetableEntry().getId())) {
                    // This is the template entry we are overriding. Skip it because we are replacing it!
                    continue;
                }
                if (slot.isCancelled()) {
                    continue; // Cancelled slots don't conflict
                }
                // Check time overlap
                if (isOverlapping(start, end, slot.getStartTime(), slot.getEndTime())) {
                    throw new IllegalStateException(String.format("Faculty %s is already busy teaching %s from %s to %s.",
                            targetFaculty.getUser() != null ? targetFaculty.getUser().getName() : "Faculty",
                            slot.getSubjectName(), slot.getStartTime(), slot.getEndTime()));
                }
            }
        }

        // 2. Validate Room Conflict (if a room is set)
        Room targetRoom = candidate.getNewRoom() != null ? candidate.getNewRoom() : 
                (candidate.getTimetableEntry() != null ? candidate.getTimetableEntry().getRoom() : null);

        if (targetRoom != null && candidate.getOverrideType() != com.ucmp.ucmp_backend.model.OverrideType.CANCELLED) {
            // Check room overlaps
            // We can resolve the schedule of the room by checking all sections' resolved schedules or checking active overrides
            // For simplicity, check resolved schedules for all sections, or we can query overrides in the room
            // Let's resolve the schedule for the section first, and if room is occupied by another section:
            for (Section section : candidate.getSections()) {
                List<TimetableEntryResponseDTO> sectionSchedule = resolutionService.getResolvedScheduleForSection(
                        section.getId(), candidate.getOverrideDate(), term);
                
                for (TimetableEntryResponseDTO slot : sectionSchedule) {
                    if (candidate.getId() != null && slot.getOverrideId() != null && slot.getOverrideId().equals(candidate.getId())) {
                        continue;
                    }
                    if (candidate.getTimetableEntry() != null && slot.getId() != null && slot.getId().equals(candidate.getTimetableEntry().getId())) {
                        continue;
                    }
                    if (slot.isCancelled()) {
                        continue;
                    }
                    // Check if it's the same room
                    if (slot.getRoomId() != null && slot.getRoomId().equals(targetRoom.getId())) {
                        if (isOverlapping(start, end, slot.getStartTime(), slot.getEndTime())) {
                            throw new IllegalStateException(String.format("Room %s is already hosting %s from %s to %s.",
                                    targetRoom.getName(), slot.getSubjectName(), slot.getStartTime(), slot.getEndTime()));
                        }
                    }
                }
            }
        }

        // 3. Validate Section Conflict
        if (candidate.getSections() != null && candidate.getOverrideType() != com.ucmp.ucmp_backend.model.OverrideType.CANCELLED) {
            for (Section section : candidate.getSections()) {
                List<TimetableEntryResponseDTO> sectionSchedule = resolutionService.getResolvedScheduleForSection(
                        section.getId(), candidate.getOverrideDate(), term);

                for (TimetableEntryResponseDTO slot : sectionSchedule) {
                    if (candidate.getId() != null && slot.getOverrideId() != null && slot.getOverrideId().equals(candidate.getId())) {
                        continue;
                    }
                    if (candidate.getTimetableEntry() != null && slot.getId() != null && slot.getId().equals(candidate.getTimetableEntry().getId())) {
                        continue;
                    }
                    if (slot.isCancelled()) {
                        continue;
                    }
                    if (isOverlapping(start, end, slot.getStartTime(), slot.getEndTime())) {
                        throw new IllegalStateException(String.format("Section %s has another class (%s) scheduled from %s to %s.",
                                section.getSectionName(), slot.getSubjectName(), slot.getStartTime(), slot.getEndTime()));
                    }
                }
            }
        }
    }

    private boolean isOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
