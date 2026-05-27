package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.*;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import com.ucmp.ucmp_backend.validator.TimetableOverrideValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.ucmp.ucmp_backend.dto.websocket.ClassCancelledEvent;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableRepo;
    private final SubjectRepository subjectRepo;
    private final RoomRepository roomRepo;
    private final SectionRepository sectionRepo;
    private final FacultyRepository facultyRepo;
    private final SubjectAssignmentRepository assignmentRepo;
    private final ClassCancellationRepository cancellationRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnnouncementRepository announcementRepo;
    private final TimetableOverrideRepository overrideRepo;
    private final TimetableOverrideValidator overrideValidator;
    private final TimetableResolutionService resolutionService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final org.springframework.cache.CacheManager cacheManager;

    // ─────────────────────────────────────────────────────────────────────
    // CONFLICT VALIDATION — runs BEFORE any save
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Check if a proposed timetable entry would cause any conflicts.
     * Covers all 3 hard constraints from the plan:
     * #1 Faculty double-booking
     * #2 Room double-booking
     * #3 Section double-booking
     * Also validates lab slot integrity (no crossing lunch break).
     *
     * @param excludeId pass the entry's own ID when updating, null when creating
     */
    public ConflictCheckResult validateEntry(
            Long sectionId, Long facultyId, Long roomId, Long subjectId,
            DayOfWeek day, LocalTime startTime, LocalTime endTime,
            String academicTerm, Long excludeId) {

        List<String> conflicts = new ArrayList<>();

        // Fetch names for readable error messages
        String sectionName = sectionRepo.findById(sectionId).map(Section::getSectionName)
                .orElse("Section#" + sectionId);
        String facultyName = facultyRepo.findById(facultyId)
                .map(f -> f.getUser() != null ? f.getUser().getName() : "Faculty#" + facultyId)
                .orElse("Faculty#" + facultyId);
        String roomName = roomRepo.findById(roomId).map(Room::getName).orElse("Room#" + roomId);

        // Hard Constraint #1: Faculty double-booking
        boolean facultyBusy = timetableRepo
                .findByFacultyIdAndDayAndAcademicTermOrderByStartTimeAsc(facultyId, day, academicTerm)
                .stream()
                .filter(e -> excludeId == null || !e.getId().equals(excludeId))
                .anyMatch(e -> timesOverlap(startTime, endTime, e.getStartTime(), e.getEndTime()));

        if (facultyBusy) {
            conflicts.add("Faculty '" + facultyName + "' is already teaching another class on "
                    + day + " between " + startTime + " and " + endTime);
        }

        // Hard Constraint #2: Section double-booking
        boolean sectionBusy = timetableRepo
                .findBySectionIdAndAcademicTermAndDay(sectionId, academicTerm, day)
                .stream()
                .filter(e -> excludeId == null || !e.getId().equals(excludeId))
                .anyMatch(e -> timesOverlap(startTime, endTime, e.getStartTime(), e.getEndTime()));

        if (sectionBusy) {
            conflicts.add("Section '" + sectionName + "' already has a class on "
                    + day + " between " + startTime + " and " + endTime);
        }

        // Hard Constraint #3: Room double-booking
        boolean roomBusy = timetableRepo
                .findByRoomIdAndAcademicTermOrderByDayAscStartTimeAsc(roomId, academicTerm)
                .stream()
                .filter(e -> e.getDay() == day)
                .filter(e -> excludeId == null || !e.getId().equals(excludeId))
                .anyMatch(e -> timesOverlap(startTime, endTime, e.getStartTime(), e.getEndTime()));

        if (roomBusy) {
            conflicts.add("Room '" + roomName + "' is already occupied on "
                    + day + " between " + startTime + " and " + endTime);
        }

        // Hard Constraint #4: Lab slot cannot cross lunch break (13:00–14:00)
        LocalTime lunchStart = LocalTime.of(13, 0);
        LocalTime lunchEnd = LocalTime.of(14, 0);
        if (startTime.isBefore(lunchEnd) && endTime.isAfter(lunchStart)) {
            conflicts.add("Class slot " + startTime + "–" + endTime
                    + " overlaps with the lunch break (13:00–14:00)");
        }

        // Room capacity check
        roomRepo.findById(roomId).ifPresent(room -> {
            sectionRepo.findById(sectionId).ifPresent(section -> {
                int sectionSize = section.getStudents().size();
                if (sectionSize > 0 && room.getCapacity() < sectionSize) {
                    conflicts.add("Room '" + room.getName() + "' has capacity " + room.getCapacity()
                            + " but section '" + sectionName + "' has " + sectionSize + " students");
                }
            });
        });

        // Room type compatibility check
        subjectRepo.findById(subjectId).ifPresent(subject -> {
            roomRepo.findById(roomId).ifPresent(room -> {
                if (subject.getRequiredRoomType() != RoomType.ANY
                        && room.getType() != subject.getRequiredRoomType()) {
                    conflicts.add("Subject '" + subject.getName() + "' requires a "
                            + subject.getRequiredRoomType() + " but '" + room.getName()
                            + "' is a " + room.getType());
                }
            });
        });

        // Room availability
        roomRepo.findById(roomId).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                conflicts.add("Room '" + room.getName() + "' is currently " + room.getStatus()
                        + " and cannot be booked");
            }
        });

        return conflicts.isEmpty()
                ? ConflictCheckResult.clean()
                : ConflictCheckResult.withConflicts(conflicts);
    }

    /** Check if two time ranges overlap */
    private boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CRUD OPERATIONS
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public TimetableEntryResponseDTO createEntry(CreateTimetableEntryRequest req) {
        // 1. Validate first
        ConflictCheckResult check = validateEntry(
                req.getSectionId(), req.getFacultyId(), req.getRoomId(), req.getSubjectId(),
                req.getDay(), req.getStartTime(), req.getEndTime(),
                req.getAcademicTerm(), null);

        if (check.isHasConflicts()) {
            throw new IllegalStateException("Conflict detected: " + String.join("; ", check.getConflicts()));
        }

        // 2. Load entities
        Subject subject = subjectRepo.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found: " + req.getSubjectId()));
        Room room = roomRepo.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));
        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found: " + req.getSectionId()));
        Faculty faculty = facultyRepo.findById(req.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + req.getFacultyId()));

        // 3. Build and save
        TimetableEntry entry = TimetableEntry.builder()
                .day(req.getDay())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .subject(subject)
                .room(room)
                .section(section)
                .faculty(faculty)
                .academicTerm(req.getAcademicTerm())
                .entryType(req.getEntryType() != null ? req.getEntryType() : EntryType.REGULAR)
                .build();

        TimetableEntry saved = timetableRepo.save(entry);

        // Clear resolved schedule caches since templates are modified
        try {
            org.springframework.cache.Cache sectionCache = cacheManager.getCache("resolved_section_schedules");
            if (sectionCache != null) {
                sectionCache.clear();
            }
            org.springframework.cache.Cache facultyCache = cacheManager.getCache("resolved_faculty_schedules");
            if (facultyCache != null) {
                facultyCache.clear();
            }
        } catch (Exception ex) {
            System.err.println("Failed to clear cache on template creation: " + ex.getMessage());
        }

        return toDTO(saved);
    }

    @Transactional
    public TimetableEntryResponseDTO updateEntry(Long id, CreateTimetableEntryRequest req) {
        TimetableEntry existing = timetableRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Timetable entry not found: " + id));

        // Validate with excludeId so entry doesn't conflict with itself
        ConflictCheckResult check = validateEntry(
                req.getSectionId(), req.getFacultyId(), req.getRoomId(), req.getSubjectId(),
                req.getDay(), req.getStartTime(), req.getEndTime(),
                req.getAcademicTerm(), id);

        if (check.isHasConflicts()) {
            throw new IllegalStateException("Conflict detected: " + String.join("; ", check.getConflicts()));
        }

        existing.setDay(req.getDay());
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        existing.setSubject(subjectRepo.findById(req.getSubjectId()).orElseThrow());
        existing.setRoom(roomRepo.findById(req.getRoomId()).orElseThrow());
        existing.setSection(sectionRepo.findById(req.getSectionId()).orElseThrow());
        existing.setFaculty(facultyRepo.findById(req.getFacultyId()).orElseThrow());
        existing.setAcademicTerm(req.getAcademicTerm());
        if (req.getEntryType() != null)
            existing.setEntryType(req.getEntryType());

        TimetableEntry saved = timetableRepo.save(existing);

        // Clear resolved schedule caches since templates are modified
        try {
            org.springframework.cache.Cache sectionCache = cacheManager.getCache("resolved_section_schedules");
            if (sectionCache != null) {
                sectionCache.clear();
            }
            org.springframework.cache.Cache facultyCache = cacheManager.getCache("resolved_faculty_schedules");
            if (facultyCache != null) {
                facultyCache.clear();
            }
        } catch (Exception ex) {
            System.err.println("Failed to clear cache on template update: " + ex.getMessage());
        }

        return toDTO(saved);
    }

    @Transactional
    public void deleteEntry(Long id) {
        if (!timetableRepo.existsById(id)) {
            throw new RuntimeException("Timetable entry not found: " + id);
        }
        timetableRepo.deleteById(id);

        // Clear resolved schedule caches since templates are modified
        try {
            org.springframework.cache.Cache sectionCache = cacheManager.getCache("resolved_section_schedules");
            if (sectionCache != null) {
                sectionCache.clear();
            }
            org.springframework.cache.Cache facultyCache = cacheManager.getCache("resolved_faculty_schedules");
            if (facultyCache != null) {
                facultyCache.clear();
            }
        } catch (Exception ex) {
            System.err.println("Failed to clear cache on template deletion: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // QUERY OPERATIONS
    // ─────────────────────────────────────────────────────────────────────

    public List<TimetableEntryResponseDTO> getScheduleForSection(Long sectionId, String academicTerm) {
        return timetableRepo
                .findBySectionIdAndAcademicTermOrderByDayAscStartTimeAsc(sectionId, academicTerm)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TimetableEntryResponseDTO> getScheduleForFaculty(Long facultyId, String academicTerm) {
        return timetableRepo
                .findByFacultyIdAndAcademicTermOrderByDayAscStartTimeAsc(facultyId, academicTerm)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TimetableEntryResponseDTO> getScheduleForRoom(Long roomId, String academicTerm) {
        return timetableRepo
                .findByRoomIdAndAcademicTermOrderByDayAscStartTimeAsc(roomId, academicTerm)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUBJECT ASSIGNMENT CRUD
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public SubjectAssignment createAssignment(SubjectAssignmentRequest req) {
        if (assignmentRepo.existsByFacultyIdAndSubjectIdAndSectionIdAndAcademicTerm(
                req.getFacultyId(), req.getSubjectId(), req.getSectionId(), req.getAcademicTerm())) {
            throw new IllegalStateException(
                    "This faculty is already assigned this subject to this section in term " + req.getAcademicTerm());
        }

        Subject subject = subjectRepo.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        Faculty faculty = facultyRepo.findById(req.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found"));

        SubjectAssignment assignment = SubjectAssignment.builder()
                .subject(subject)
                .faculty(faculty)
                .section(section)
                .academicTerm(req.getAcademicTerm())
                .weeklySlots(req.getWeeklySlots())
                .build();

        return assignmentRepo.save(assignment);
    }

    public List<SubjectAssignment> getAssignmentsForTerm(String academicTerm) {
        return assignmentRepo.findByAcademicTerm(academicTerm);
    }

    public List<SubjectAssignment> getAssignmentsForSection(Long sectionId, String academicTerm) {
        return assignmentRepo.findBySectionIdAndAcademicTerm(sectionId, academicTerm);
    }

    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignmentRepo.existsById(id))
            throw new RuntimeException("Assignment not found: " + id);
        assignmentRepo.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER: Entity → DTO
    // ─────────────────────────────────────────────────────────────────────

    private TimetableEntryResponseDTO toDTO(TimetableEntry e) {
        String facultyName = (e.getFaculty() != null && e.getFaculty().getUser() != null)
                ? e.getFaculty().getUser().getName()
                : "Unknown";

        return TimetableEntryResponseDTO.builder()
                .id(e.getId())
                .day(e.getDay())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .subjectId(e.getSubject().getId())
                .subjectName(e.getSubject().getName())
                .subjectCode(e.getSubject().getCode())
                .roomId(e.getRoom().getId())
                .roomName(e.getRoom().getName())
                .roomBuilding(e.getRoom().getBuilding())
                .sectionId(e.getSection().getId())
                .sectionName(e.getSection().getSectionName())
                .facultyId(e.getFaculty().getId())
                .facultyName(facultyName)
                .academicTerm(e.getAcademicTerm())
                .entryType(e.getEntryType())
                .build();
    }

    @Transactional
    public ClassCancellation cancelEntry(Long entryId, LocalDate date, String reason, String facultyCollegeId) {
        TimetableEntry entry = timetableRepo.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable entry not found"));

        Faculty faculty = facultyRepo.findByCollegeId(facultyCollegeId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

        // Validate that this faculty is indeed assigned to this timetable entry
        if (!entry.getFaculty().getId().equals(faculty.getId())) {
            throw new IllegalStateException("You can only cancel classes assigned to you.");
        }

        // Check if already cancelled via ClassCancellation
        Optional<ClassCancellation> existing = cancellationRepo
                .findByTimetableEntryIdAndCancellationDate(entryId, date);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Dedup: check if admin already cancelled this slot via TimetableOverride
        List<TimetableOverride> existingOverrides = overrideRepo.findActiveOverridesByEntryAndDate(entryId, date);
        boolean alreadyOverrideCancelled = existingOverrides.stream()
                .anyMatch(o -> o.getOverrideType() == OverrideType.CANCELLED);
        if (alreadyOverrideCancelled) {
            throw new IllegalStateException("This class slot is already cancelled by admin for " + date
                    + ". No additional cancellation is needed.");
        }

        ClassCancellation cancellation = ClassCancellation.builder()
                .timetableEntry(entry)
                .cancellationDate(date)
                .cancelledBy(faculty)
                .reason(reason)
                .approvalStatus(CancellationStatus.AUTO_APPROVED)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        ClassCancellation saved = cancellationRepo.save(cancellation);

        // Push WebSocket update & save persistent announcement
        try {
            ClassCancelledEvent event = new ClassCancelledEvent(
                    entry.getId(),
                    entry.getSubject().getName(),
                    entry.getSubject().getCode(),
                    date,
                    entry.getStartTime() + " - " + entry.getEndTime(),
                    reason);
            messagingTemplate.convertAndSend("/topic/cancellation/" + entry.getSection().getId(), event);
        } catch (Exception e) {
            System.err.println("Failed to broadcast ClassCancelledEvent: " + e.getMessage());
        }

        try {
            Announcements cancellationAnnouncement = new Announcements();
            cancellationAnnouncement.setTitle("Class Cancelled: " + entry.getSubject().getName());
            cancellationAnnouncement.setDescription(String.format("The class on %s (%s) has been cancelled. Reason: %s",
                    date.toString(),
                    entry.getStartTime() + " - " + entry.getEndTime(),
                    reason));
            cancellationAnnouncement.setAuthor(faculty.getUser() != null ? faculty.getUser().getName() : "Faculty");
            cancellationAnnouncement.setTime(java.time.LocalDateTime.now().toString());
            cancellationAnnouncement.setType("TIMETABLE");
            cancellationAnnouncement.setSectionId(entry.getSection().getId());
            cancellationAnnouncement.setCompleted(false);

            announcementRepo.save(cancellationAnnouncement);

            // Broadcast to notifications channel
            messagingTemplate.convertAndSend("/topic/notifications/section/" + entry.getSection().getId(),
                    cancellationAnnouncement);
        } catch (Exception e) {
            System.err.println("Failed to create and broadcast cancellation announcement: " + e.getMessage());
        }

        // Evict resolved schedules cache for this section and faculty on the cancelled
        // date
        try {
            org.springframework.cache.Cache sectionCache = cacheManager.getCache("resolved_section_schedules");
            if (sectionCache != null) {
                sectionCache.evict(entry.getSection().getId() + ":" + date);
            }
            org.springframework.cache.Cache facultyCache = cacheManager.getCache("resolved_faculty_schedules");
            if (facultyCache != null) {
                facultyCache.evict(entry.getFaculty().getId() + ":" + date);
            }
        } catch (Exception ex) {
            System.err.println("Failed to evict cache on cancelEntry: " + ex.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<FacultyAvailabilityResponse> getFacultyAvailability(
            LocalDate date, LocalTime startTime, LocalTime endTime, Long subjectId, String term) {

        Subject subject = (subjectId != null) ? subjectRepo.findById(subjectId).orElse(null) : null;
        String dept = (subject != null) ? subject.getDepartment() : null;

        // Step 1: Pre-filter candidates by department or subject expertise
        List<Faculty> allFaculties = facultyRepo.findAll();

        // Find who is assigned to teach this subject in subject_assignments
        Set<Long> directExpertiseFacultyIds = new java.util.HashSet<>();
        if (subjectId != null) {
            directExpertiseFacultyIds.addAll(assignmentRepo.findBySubjectId(subjectId).stream()
                    .map(sa -> sa.getFaculty().getId())
                    .collect(Collectors.toSet()));
        }

        List<FacultyAvailabilityResponse> results = new ArrayList<>();

        for (Faculty faculty : allFaculties) {
            boolean departmentMatch = dept != null && dept.equalsIgnoreCase(faculty.getDepartment());
            boolean directExpertise = directExpertiseFacultyIds.contains(faculty.getId());

            if (dept != null && !departmentMatch && !directExpertise) {
                // Pre-filter out unrelated departments
                continue;
            }

            // Step 2: Check if faculty is busy at [startTime, endTime] on date
            List<TimetableEntryResponseDTO> resolvedSchedule = resolutionService
                    .getResolvedScheduleForFaculty(faculty.getId(), date, term);

            boolean isBusy = false;
            String conflictDesc = "";

            for (TimetableEntryResponseDTO slot : resolvedSchedule) {
                if (slot.isCancelled()) {
                    continue;
                }
                if (timesOverlap(startTime, endTime, slot.getStartTime(), slot.getEndTime())) {
                    isBusy = true;
                    conflictDesc = "Teaching " + slot.getSubjectName() + " (" + slot.getStartTime() + " - "
                            + slot.getEndTime() + ")";
                    break;
                }
            }

            // Step 3: Compute workload metrics
            long totalLoadSlots = timetableRepo.countByFacultyIdAndAcademicTerm(faculty.getId(), term);

            // Step 4: Count recent substitutions
            long recentSubCount = overrideRepo.findByNewFacultyIdAndOverrideDate(faculty.getId(), date).stream()
                    .filter(o -> o.getOverrideType() == OverrideType.SUBSTITUTE)
                    .count();

            // Expertise rank mapping
            String expertiseRank = "General Backup";
            if (directExpertise) {
                expertiseRank = "Direct Expertise";
            } else if (departmentMatch) {
                expertiseRank = "Departmental Match";
            }

            results.add(FacultyAvailabilityResponse.builder()
                    .facultyId(faculty.getId())
                    .facultyName(faculty.getUser() != null ? faculty.getUser().getName() : "Faculty")
                    .collegeId(faculty.getCollegeId())
                    .department(faculty.getDepartment())
                    .designation(faculty.getDesignation())
                    .status(isBusy ? "BUSY" : "FREE")
                    .conflictDescription(conflictDesc)
                    .expertiseRank(expertiseRank)
                    .weeklyWorkloadSlots(totalLoadSlots)
                    .recentSubstitutionCount(recentSubCount)
                    .build());
        }

        // Sort by FREE first, then expertise, then workload
        results.sort((a, b) -> {
            if (!a.getStatus().equals(b.getStatus())) {
                return a.getStatus().compareTo(b.getStatus()); // FREE comes before BUSY ("FREE".compareTo("BUSY") is
                                                               // negative)
            }

            int rankA = getExpertiseRankPriority(a.getExpertiseRank());
            int rankB = getExpertiseRankPriority(b.getExpertiseRank());
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }

            return Long.compare(a.getWeeklyWorkloadSlots(), b.getWeeklyWorkloadSlots());
        });

        return results;
    }

    private int getExpertiseRankPriority(String rank) {
        if ("Direct Expertise".equals(rank))
            return 1;
        if ("Departmental Match".equals(rank))
            return 2;
        return 3;
    }

    @Transactional
    public TimetableOverride createOverride(com.ucmp.ucmp_backend.dto.TimetableOverrideRequestDTO dto) {
        LocalDate date = LocalDate.parse(dto.getOverrideDate());
        OverrideType type = OverrideType.valueOf(dto.getOverrideType());

        TimetableEntry templateEntry = null;
        if (dto.getTimetableEntryId() != null) {
            templateEntry = timetableRepo.findById(dto.getTimetableEntryId())
                    .orElseThrow(() -> new RuntimeException("Timetable entry template not found"));
        }

        // Dedup: prevent admin CANCELLED override if faculty already self-cancelled
        // this slot
        if (type == OverrideType.CANCELLED && templateEntry != null) {
            boolean alreadySelfCancelled = cancellationRepo
                    .findByTimetableEntryIdAndCancellationDate(templateEntry.getId(), date)
                    .filter(cc -> cc.isEffective())
                    .isPresent();
            if (alreadySelfCancelled) {
                throw new IllegalStateException("This class slot is already cancelled by the faculty for " + date
                        + ". No additional cancellation override is needed.");
            }
        }

        Faculty originalFaculty = null;
        if (templateEntry != null) {
            originalFaculty = templateEntry.getFaculty();
        }

        Faculty newFaculty = null;
        if (dto.getNewFacultyId() != null) {
            newFaculty = facultyRepo.findById(dto.getNewFacultyId())
                    .orElseThrow(() -> new RuntimeException("New Faculty not found"));
        }

        Room originalRoom = null;
        if (templateEntry != null) {
            originalRoom = templateEntry.getRoom();
        }

        Room newRoom = null;
        if (dto.getNewRoomId() != null) {
            newRoom = roomRepo.findById(dto.getNewRoomId())
                    .orElseThrow(() -> new RuntimeException("New Room not found"));
        }

        Subject subject = null;
        if (dto.getSubjectId() != null) {
            subject = subjectRepo.findById(dto.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));
        } else if (templateEntry != null) {
            subject = templateEntry.getSubject();
        }

        List<Section> sections = new ArrayList<>();
        if (dto.getSectionIds() != null && !dto.getSectionIds().isEmpty()) {
            sections = sectionRepo.findAllById(dto.getSectionIds());
        } else if (templateEntry != null) {
            sections = List.of(templateEntry.getSection());
        }

        LocalTime origStart = templateEntry != null ? templateEntry.getStartTime() : null;
        LocalTime origEnd = templateEntry != null ? templateEntry.getEndTime() : null;

        LocalTime newStart = dto.getNewStartTime() != null ? LocalTime.parse(dto.getNewStartTime()) : origStart;
        LocalTime newEnd = dto.getNewEndTime() != null ? LocalTime.parse(dto.getNewEndTime()) : origEnd;

        LocalDate effectiveFrom = dto.getEffectiveFrom() != null ? LocalDate.parse(dto.getEffectiveFrom()) : null;
        LocalDate effectiveTo = dto.getEffectiveTo() != null ? LocalDate.parse(dto.getEffectiveTo()) : null;
        boolean isRecurring = dto.getIsRecurring() != null ? dto.getIsRecurring() : false;
        String recurringPattern = dto.getRecurringPattern();

        OverrideStatus status = dto.getStatus() != null ? OverrideStatus.valueOf(dto.getStatus())
                : OverrideStatus.ACTIVE;

        TimetableOverride override = TimetableOverride.builder()
                .timetableEntry(templateEntry)
                .overrideDate(date)
                .overrideType(type)
                .originalFaculty(originalFaculty)
                .newFaculty(newFaculty)
                .originalRoom(originalRoom)
                .newRoom(newRoom)
                .originalStartTime(origStart)
                .newStartTime(newStart)
                .originalEndTime(origEnd)
                .newEndTime(newEnd)
                .subject(subject)
                .sections(sections)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .isRecurring(isRecurring)
                .recurringPattern(recurringPattern)
                .status(status)
                .reason(dto.getReason())
                .build();

        // Run validation
        overrideValidator.validate(override, dto.getAcademicTerm());

        TimetableOverride saved = overrideRepo.save(override);

        // Publish event to trigger listeners
        List<Long> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toList());
        eventPublisher.publishEvent(new com.ucmp.ucmp_backend.event.TimetableOverrideEvent(
                saved.getId(),
                saved.getOverrideType().name(),
                saved.getOverrideDate(),
                sectionIds,
                saved.getNewFaculty() != null ? saved.getNewFaculty().getId() : null,
                saved.getOriginalFaculty() != null ? saved.getOriginalFaculty().getId() : null,
                saved.getReason()));

        return saved;
    }

    @Transactional
    public TimetableOverride cancelOverride(Long overrideId) {
        TimetableOverride override = overrideRepo.findById(overrideId)
                .orElseThrow(() -> new RuntimeException("Override not found"));
        override.setStatus(OverrideStatus.CANCELLED);
        TimetableOverride saved = overrideRepo.save(override);

        List<Long> sectionIds = override.getSections().stream().map(Section::getId).collect(Collectors.toList());
        eventPublisher.publishEvent(new com.ucmp.ucmp_backend.event.TimetableOverrideEvent(
                saved.getId(),
                "CANCEL_OVERRIDE",
                saved.getOverrideDate(),
                sectionIds,
                null,
                null,
                "Override Cancelled/Reverted"));

        return saved;
    }

    @Transactional(readOnly = true)
    public AocsMetricsResponse getAocsMetrics(String term) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        java.time.DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 1. Active overrides today (single query)
        List<TimetableOverride> todaysOverrides = overrideRepo.findActiveOverridesByDate(today);
        long activeOverridesCount = todaysOverrides.size();

        // 2. Pending substitutions (aggregate COUNT query — no more findAll)
        long pendingReplacementsCount = overrideRepo.countPendingByTerm(term);

        // 3. Cancelled classes count (aggregate queries — no more findAll)
        long cancelledOverridesCount = overrideRepo.countCancelledOverridesByDate(today);
        long selfCancelledCount = cancellationRepo.countEffectiveCancellationsByDate(today);
        long cancelledClassesCount = cancelledOverridesCount + selfCancelledCount;

        // 4. Live ongoing lectures (lightweight DB count on templates — no per-section
        // resolution)
        // This is an approximation: it counts template entries currently in progress
        // without
        // accounting for overrides/cancellations, but is fast and close enough for a
        // dashboard metric.
        long liveOngoingLecturesCount = timetableRepo.countOngoingByDayAndTime(dayOfWeek, term, now);

        // 5. Faculty Utilization Rate today (lightweight DB count — no per-faculty
        // resolution)
        long busyFacultiesCount = timetableRepo.countDistinctFacultiesWithClassOnDay(dayOfWeek, term);
        long totalFaculties = facultyRepo.count();
        double facultyUtilizationRate = totalFaculties == 0 ? 0.0
                : Math.round(((double) busyFacultiesCount / totalFaculties * 100.0) * 10.0) / 10.0;

        return AocsMetricsResponse.builder()
                .activeOverridesCount(activeOverridesCount)
                .pendingReplacementsCount(pendingReplacementsCount)
                .facultyUtilizationRate(facultyUtilizationRate)
                .cancelledClassesCount(cancelledClassesCount)
                .liveOngoingLecturesCount(liveOngoingLecturesCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctAcademicTerms() {
        return timetableRepo.findDistinctAcademicTerms();
    }
}
