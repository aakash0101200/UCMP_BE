package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.TimetableEntryResponseDTO;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.ClassCancellationRepository;
import com.ucmp.ucmp_backend.repository.TimetableEntryRepository;
import com.ucmp.ucmp_backend.repository.TimetableOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableResolutionService {

    private final TimetableEntryRepository entryRepository;
    private final TimetableOverrideRepository overrideRepository;
    private final ClassCancellationRepository cancellationRepository;

    /**
     * Resolves the schedule for a section on a specific date, merging recurring templates & overrides.
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "resolved_section_schedules", key = "#sectionId.toString().concat(':').concat(#date.toString())")
    public List<TimetableEntryResponseDTO> getResolvedScheduleForSection(Long sectionId, LocalDate date, String term) {
        DayOfWeek day = date.getDayOfWeek();
        List<TimetableEntry> templates = entryRepository.findBySectionIdAndAcademicTermAndDay(sectionId, term, day);
        List<TimetableOverride> overrides = overrideRepository.findActiveOverridesBySectionAndDate(sectionId, date);
        overrides = filterOverridesByDate(overrides, date);

        List<TimetableEntryResponseDTO> resolvedList = new ArrayList<>();

        // Map template entry ID to active overrides affecting it
        Map<Long, List<TimetableOverride>> entryOverridesMap = overrides.stream()
                .filter(o -> o.getTimetableEntry() != null)
                .collect(Collectors.groupingBy(o -> o.getTimetableEntry().getId()));

        // 1. Process templates and apply overrides
        for (TimetableEntry entry : templates) {
            List<TimetableOverride> entryOverrides = entryOverridesMap.getOrDefault(entry.getId(), Collections.emptyList());
            resolvedList.add(resolveEntry(entry, entryOverrides, date));
        }

        // 2. Add extra classes (overrides with no template entry)
        List<TimetableOverride> extraClasses = overrides.stream()
                .filter(o -> o.getTimetableEntry() == null && o.getOverrideType() == OverrideType.EXTRA_CLASS)
                .toList();

        for (TimetableOverride extra : extraClasses) {
            resolvedList.add(resolveExtraClass(extra, date));
        }

        // Sort by start time
        resolvedList.sort(Comparator.comparing(TimetableEntryResponseDTO::getStartTime));
        return resolvedList;
    }

    /**
     * Resolves the schedule for a faculty on a specific date.
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "resolved_faculty_schedules", key = "#facultyId.toString().concat(':').concat(#date.toString())")
    public List<TimetableEntryResponseDTO> getResolvedScheduleForFaculty(Long facultyId, LocalDate date, String term) {
        DayOfWeek day = date.getDayOfWeek();
        
        List<TimetableEntry> templates = entryRepository.findByFacultyIdAndDayAndAcademicTermOrderByStartTimeAsc(facultyId, day, term);
        
        // 2. Get all active overrides for this faculty on this date
        List<TimetableOverride> overrides = overrideRepository.findActiveOverridesByFacultyAndDate(facultyId, date);
        overrides = filterOverridesByDate(overrides, date);
        
        List<TimetableEntryResponseDTO> resolvedList = new ArrayList<>();

        // Process templates originally assigned to this faculty
        for (TimetableEntry entry : templates) {
            // Find overrides for this template entry
            List<TimetableOverride> entryOverrides = overrides.stream()
                    .filter(o -> o.getTimetableEntry() != null && o.getTimetableEntry().getId().equals(entry.getId()))
                    .toList();
            
            TimetableEntryResponseDTO resolved = resolveEntry(entry, entryOverrides, date);
            
            // Even if substituted out, we include it but mark it as substituted with isSubstituted=true and facultyId != facultyId.
            resolvedList.add(resolved);
        }

        // 3. Add classes where this faculty is substituted IN (newFacultyId == facultyId) and NOT originally their class
        List<TimetableOverride> substitutionIn = overrides.stream()
                .filter(o -> o.getNewFaculty() != null && o.getNewFaculty().getId().equals(facultyId))
                .filter(o -> o.getTimetableEntry() != null && !o.getTimetableEntry().getFaculty().getId().equals(facultyId))
                .toList();

        for (TimetableOverride sub : substitutionIn) {
            // We resolve the template entry but overwrite the faculty to be this covering faculty
            TimetableEntry entry = sub.getTimetableEntry();
            List<TimetableOverride> entryOverrides = List.of(sub); // Just this override
            resolvedList.add(resolveEntry(entry, entryOverrides, date));
        }

        // 4. Add extra classes where this faculty is scheduled to teach
        List<TimetableOverride> extraClasses = overrides.stream()
                .filter(o -> o.getTimetableEntry() == null && o.getOverrideType() == OverrideType.EXTRA_CLASS)
                .filter(o -> o.getNewFaculty() != null && o.getNewFaculty().getId().equals(facultyId))
                .toList();

        for (TimetableOverride extra : extraClasses) {
            resolvedList.add(resolveExtraClass(extra, date));
        }

        resolvedList.sort(Comparator.comparing(TimetableEntryResponseDTO::getStartTime));
        return resolvedList;
    }

    /**
     * Helper to resolve an entry by merging template and overrides in priority order.
     */
    private TimetableEntryResponseDTO resolveEntry(TimetableEntry entry, List<TimetableOverride> overrides, LocalDate date) {
        TimetableEntryResponseDTO.TimetableEntryResponseDTOBuilder builder = TimetableEntryResponseDTO.builder()
                .id(entry.getId())
                .day(entry.getDay())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .subjectId(entry.getSubject().getId())
                .subjectName(entry.getSubject().getName())
                .subjectCode(entry.getSubject().getCode())
                .roomId(entry.getRoom().getId())
                .roomName(entry.getRoom().getName())
                .roomBuilding(entry.getRoom().getBuilding())
                .sectionId(entry.getSection().getId())
                .sectionName(entry.getSection().getSectionName())
                .facultyId(entry.getFaculty().getId())
                .facultyName(entry.getFaculty().getUser() != null ? entry.getFaculty().getUser().getName() : "Faculty")
                .academicTerm(entry.getAcademicTerm())
                .entryType(entry.getEntryType())
                .resolvedDate(date)
                .isOverride(false);

        List<Long> appliedOverrideIds = new ArrayList<>();
        List<String> appliedOverrideTypes = new ArrayList<>();

        // Check for cancellation
        boolean effectivelyCancelled = isEffectivelyCancelled(entry.getId(), overrides, date);

        if (effectivelyCancelled) {
            Optional<TimetableOverride> cancelledOpt = overrides.stream()
                    .filter(o -> o.getOverrideType() == OverrideType.CANCELLED)
                    .findFirst();
            if (cancelledOpt.isPresent()) {
                TimetableOverride cancelOverride = cancelledOpt.get();
                appliedOverrideIds.add(cancelOverride.getId());
                builder.isOverride(true)
                       .isCancelled(true)
                       .overrideType(OverrideType.CANCELLED.name())
                       .overrideId(cancelOverride.getId())
                       .overrideReason(cancelOverride.getReason())
                       .appliedOverrideIds(appliedOverrideIds);
            } else {
                builder.isCancelled(true)
                       .overrideReason("Faculty self-cancelled")
                       .appliedOverrideIds(appliedOverrideIds);
            }
            return builder.build();
        }

        if (overrides.isEmpty()) {
            return builder.build();
        }

        // 2. Check TIME_CHANGE
        Optional<TimetableOverride> timeChangeOpt = overrides.stream()
                .filter(o -> o.getOverrideType() == OverrideType.TIME_CHANGE)
                .findFirst();
        if (timeChangeOpt.isPresent()) {
            TimetableOverride tc = timeChangeOpt.get();
            appliedOverrideIds.add(tc.getId());
            appliedOverrideTypes.add(OverrideType.TIME_CHANGE.name());
            if (tc.getNewStartTime() != null) builder.startTime(tc.getNewStartTime());
            if (tc.getNewEndTime() != null) builder.endTime(tc.getNewEndTime());
            builder.isOverride(true)
                   .isTimeChanged(true)
                   .overrideReason(tc.getReason());
        }

        // 3. Check SUBSTITUTE
        Optional<TimetableOverride> substituteOpt = overrides.stream()
                .filter(o -> o.getOverrideType() == OverrideType.SUBSTITUTE)
                .findFirst();
        if (substituteOpt.isPresent()) {
            TimetableOverride sub = substituteOpt.get();
            appliedOverrideIds.add(sub.getId());
            appliedOverrideTypes.add(OverrideType.SUBSTITUTE.name());
            if (sub.getNewFaculty() != null) {
                builder.facultyId(sub.getNewFaculty().getId())
                       .facultyName(sub.getNewFaculty().getUser() != null ? sub.getNewFaculty().getUser().getName() : "Substitute Faculty");
            }
            builder.isOverride(true)
                   .isSubstituted(true)
                   .originalFacultyName(entry.getFaculty().getUser() != null ? entry.getFaculty().getUser().getName() : "Original Faculty")
                   .newFacultyName(sub.getNewFaculty() != null && sub.getNewFaculty().getUser() != null ? sub.getNewFaculty().getUser().getName() : "Substitute Faculty")
                   .overrideReason(sub.getReason());
        }

        // 4. Check ROOM_CHANGE
        Optional<TimetableOverride> roomChangeOpt = overrides.stream()
                .filter(o -> o.getOverrideType() == OverrideType.ROOM_CHANGE)
                .findFirst();
        if (roomChangeOpt.isPresent()) {
            TimetableOverride rc = roomChangeOpt.get();
            appliedOverrideIds.add(rc.getId());
            appliedOverrideTypes.add(OverrideType.ROOM_CHANGE.name());
            if (rc.getNewRoom() != null) {
                builder.roomId(rc.getNewRoom().getId())
                       .roomName(rc.getNewRoom().getName())
                       .roomBuilding(rc.getNewRoom().getBuilding());
            }
            builder.isOverride(true)
                   .isRoomChanged(true)
                   .originalRoomName(entry.getRoom().getName())
                   .newRoomName(rc.getNewRoom() != null ? rc.getNewRoom().getName() : "New Room")
                   .overrideReason(rc.getReason());
        }

        // 5. Check MERGED_CLASS
        Optional<TimetableOverride> mergedOpt = overrides.stream()
                .filter(o -> o.getOverrideType() == OverrideType.MERGED_CLASS)
                .findFirst();
        if (mergedOpt.isPresent()) {
            TimetableOverride mc = mergedOpt.get();
            appliedOverrideIds.add(mc.getId());
            appliedOverrideTypes.add(OverrideType.MERGED_CLASS.name());
            builder.isOverride(true)
                   .overrideReason(mc.getReason());

            List<Long> mergedSectionIds = mc.getSections().stream().map(Section::getId).collect(Collectors.toList());
            List<String> mergedSectionNames = mc.getSections().stream().map(Section::getSectionName).collect(Collectors.toList());
            builder.mergedSectionIds(mergedSectionIds)
                   .mergedSectionNames(mergedSectionNames);
        }

        // 6. Check EXTRA_CLASS
        Optional<TimetableOverride> extraClassOpt = overrides.stream()
                .filter(o -> o.getOverrideType() == OverrideType.EXTRA_CLASS)
                .findFirst();
        if (extraClassOpt.isPresent()) {
            TimetableOverride ec = extraClassOpt.get();
            appliedOverrideIds.add(ec.getId());
            appliedOverrideTypes.add(OverrideType.EXTRA_CLASS.name());
            builder.isOverride(true)
                   .overrideReason(ec.getReason());
        }

        // Set composite override metadata
        if (!appliedOverrideIds.isEmpty()) {
            builder.appliedOverrideIds(appliedOverrideIds)
                   .overrideId(appliedOverrideIds.get(appliedOverrideIds.size() - 1)) // last one for backwards compatibility
                   .overrideType(String.join(",", appliedOverrideTypes));
        }

        return builder.build();
    }

    /**
     * Checks if a timetable entry is effectively cancelled on a given date,
     * either via faculty self-cancellation or via admin CANCELLED override.
     */
    @Transactional(readOnly = true)
    public boolean isEffectivelyCancelled(Long entryId, LocalDate date) {
        List<TimetableOverride> overrides = overrideRepository.findActiveOverridesByEntryAndDate(entryId, date);
        overrides = filterOverridesByDate(overrides, date);
        return isEffectivelyCancelled(entryId, overrides, date);
    }

    /**
     * Checks if a timetable entry is effectively cancelled on a given date,
     * given the list of pre-fetched overrides for this entry/date.
     */
    @Transactional(readOnly = true)
    public boolean isEffectivelyCancelled(Long entryId, List<TimetableOverride> overrides, LocalDate date) {
        boolean selfCancelled = cancellationRepository
                .findByTimetableEntryIdAndCancellationDate(entryId, date)
                .filter(ClassCancellation::isEffective)
                .isPresent();
        if (selfCancelled) {
            return true;
        }
        return overrides.stream()
                .anyMatch(o -> o.getOverrideType() == OverrideType.CANCELLED);
    }

    /**
     * Helper to resolve an extra class override to a DTO.
     */
    private TimetableEntryResponseDTO resolveExtraClass(TimetableOverride extra, LocalDate date) {
        Faculty teacher = extra.getNewFaculty() != null ? extra.getNewFaculty() : extra.getOriginalFaculty();
        Subject sub = extra.getSubject();
        Room rm = extra.getNewRoom() != null ? extra.getNewRoom() : extra.getOriginalRoom();

        // Use the first section in the sections list as primary section for the DTO
        Long secId = null;
        String secName = "";
        if (extra.getSections() != null && !extra.getSections().isEmpty()) {
            Section primary = extra.getSections().get(0);
            secId = primary.getId();
            secName = primary.getSectionName();
        }

        List<Long> mergedSectionIds = extra.getSections() != null ? extra.getSections().stream().map(Section::getId).collect(Collectors.toList()) : Collections.emptyList();
        List<String> mergedSectionNames = extra.getSections() != null ? extra.getSections().stream().map(Section::getSectionName).collect(Collectors.toList()) : Collections.emptyList();

        return TimetableEntryResponseDTO.builder()
                .id(null) // Extra class has no static template ID
                .day(date.getDayOfWeek())
                .startTime(extra.getNewStartTime())
                .endTime(extra.getNewEndTime())
                .subjectId(sub != null ? sub.getId() : null)
                .subjectName(sub != null ? sub.getName() : "Extra Class")
                .subjectCode(sub != null ? sub.getCode() : "N/A")
                .roomId(rm != null ? rm.getId() : null)
                .roomName(rm != null ? rm.getName() : "N/A")
                .roomBuilding(rm != null ? rm.getBuilding() : "")
                .sectionId(secId)
                .sectionName(secName)
                .facultyId(teacher != null ? teacher.getId() : null)
                .facultyName(teacher != null && teacher.getUser() != null ? teacher.getUser().getName() : "Faculty")
                .academicTerm("N/A")
                .entryType(EntryType.EXTRA)
                .resolvedDate(date)
                .isOverride(true)
                .overrideType(OverrideType.EXTRA_CLASS.name())
                .overrideId(extra.getId())
                .overrideReason(extra.getReason())
                .mergedSectionIds(mergedSectionIds)
                .mergedSectionNames(mergedSectionNames)
                .build();
    }

    /**
     * Filters active overrides to only those applicable to the given date, supporting single-day and recurring patterns.
     */
    private List<TimetableOverride> filterOverridesByDate(List<TimetableOverride> overrides, LocalDate date) {
        return overrides.stream()
                .filter(o -> {
                    if (!o.isRecurring()) {
                        return o.getOverrideDate().equals(date);
                    }
                    // For recurring overrides: enforce effectiveFrom/effectiveTo boundaries
                    if (o.getEffectiveFrom() != null && date.isBefore(o.getEffectiveFrom())) {
                        return false;
                    }
                    if (o.getEffectiveTo() != null && date.isAfter(o.getEffectiveTo())) {
                        return false;
                    }
                    // Then verify day-of-week match
                    if (o.getTimetableEntry() != null) {
                        return o.getTimetableEntry().getDay() == date.getDayOfWeek();
                    } else if (o.getOverrideDate() != null) {
                        return o.getOverrideDate().getDayOfWeek() == date.getDayOfWeek();
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}
