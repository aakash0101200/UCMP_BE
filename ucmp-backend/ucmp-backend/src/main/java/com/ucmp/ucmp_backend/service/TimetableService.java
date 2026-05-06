package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.*;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableRepo;
    private final SubjectRepository subjectRepo;
    private final RoomRepository roomRepo;
    private final SectionRepository sectionRepo;
    private final FacultyRepository facultyRepo;
    private final SubjectAssignmentRepository assignmentRepo;

    // ─────────────────────────────────────────────────────────────────────
    // CONFLICT VALIDATION — runs BEFORE any save
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Check if a proposed timetable entry would cause any conflicts.
     * Covers all 3 hard constraints from the plan:
     *   #1 Faculty double-booking
     *   #2 Room double-booking
     *   #3 Section double-booking
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
        String sectionName = sectionRepo.findById(sectionId).map(Section::getSectionName).orElse("Section#" + sectionId);
        String facultyName = facultyRepo.findById(facultyId).map(f -> f.getUser() != null ? f.getUser().getName() : "Faculty#" + facultyId).orElse("Faculty#" + facultyId);
        String roomName    = roomRepo.findById(roomId).map(Room::getName).orElse("Room#" + roomId);

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
        LocalTime lunchEnd   = LocalTime.of(14, 0);
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

        return toDTO(timetableRepo.save(entry));
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
        if (req.getEntryType() != null) existing.setEntryType(req.getEntryType());

        return toDTO(timetableRepo.save(existing));
    }

    @Transactional
    public void deleteEntry(Long id) {
        if (!timetableRepo.existsById(id)) {
            throw new RuntimeException("Timetable entry not found: " + id);
        }
        timetableRepo.deleteById(id);
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
            throw new IllegalStateException("This faculty is already assigned this subject to this section in term " + req.getAcademicTerm());
        }

        Subject subject = subjectRepo.findById(req.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found"));
        Faculty faculty = facultyRepo.findById(req.getFacultyId()).orElseThrow(() -> new RuntimeException("Faculty not found"));
        Section section = sectionRepo.findById(req.getSectionId()).orElseThrow(() -> new RuntimeException("Section not found"));

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
        if (!assignmentRepo.existsById(id)) throw new RuntimeException("Assignment not found: " + id);
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
}
