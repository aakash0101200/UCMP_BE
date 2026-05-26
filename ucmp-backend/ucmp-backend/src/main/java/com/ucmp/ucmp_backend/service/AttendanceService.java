package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.AttendanceSummaryDTO;
import com.ucmp.ucmp_backend.dto.StudentAttendanceDTO;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.ucmp.ucmp_backend.dto.TimetableEntryResponseDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.ucmp.ucmp_backend.dto.websocket.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceSessionSectionRepository sessionSectionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnnouncementRepository announcementRepository;
    private final TimetableResolutionService resolutionService;

    // ── Start Session ──────────────────────────────────────────────────────────
    /**
     * @param mergedSectionIds  Additional section IDs to include (for MERGED sessions).
     *                          null or empty = REGULAR session.
     *                          The primary sectionId is always included automatically.
     */
    @Transactional
    public AttendanceSession startSession(Long facultyId, Long sectionId, Long subjectId,
                                          List<Long> mergedSectionIds,
                                          Long scheduledFacultyId,
                                          Double latitude, Double longitude,
                                          Double radiusInMeters,
                                          Integer durationInMinutes) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        Section primarySection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        // Resolve subject — nullable for backward-compat
        Subject subject = (subjectId != null)
                ? subjectRepository.findById(subjectId).orElse(null)
                : null;

        // Resolve scheduled faculty (for AOCS audit trail)
        Faculty scheduledFaculty = null;
        if (scheduledFacultyId != null) {
            scheduledFaculty = facultyRepository.findById(scheduledFacultyId).orElse(null);
        } else if (subjectId != null) {
            try {
                LocalDate today = LocalDate.now();
                // Check section's schedule for today to resolve who was scheduled
                List<TimetableEntryResponseDTO> dailySchedule = resolutionService.getResolvedScheduleForSection(sectionId, today, "2026-27-ODD");
                for (TimetableEntryResponseDTO slot : dailySchedule) {
                    if (slot.getSubjectId() != null && slot.getSubjectId().equals(subjectId)) {
                        if (slot.getFacultyId() != null) {
                            scheduledFaculty = facultyRepository.findById(slot.getFacultyId()).orElse(null);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Keep scheduledFaculty as null on failure
            }
        }

        // Determine session type
        boolean isMerged = mergedSectionIds != null && !mergedSectionIds.isEmpty();
        SessionType sessionType = isMerged ? SessionType.MERGED : SessionType.REGULAR;

        int sessionDuration = durationInMinutes != null ? durationInMinutes : 40;

        // Idempotency check: if there's an active session for the same faculty, section, and subject
        // and it was started within the chosen duration, return it.
        if (subjectId != null) {
            Optional<AttendanceSession> existingOpt = sessionRepository.findByFacultyIdAndIsActiveTrue(facultyId)
                    .stream()
                    .filter(s -> s.getSection().getId().equals(sectionId)
                            && s.getSubject() != null
                            && s.getSubject().getId().equals(subjectId)
                            && s.getStartTime().isAfter(LocalDateTime.now().minusMinutes(s.getDurationInMinutes() != null ? s.getDurationInMinutes() : 40)))
                    .findFirst();
            if (existingOpt.isPresent()) {
                return existingOpt.get();
            }
        }

        // End any existing active sessions for this faculty (keep isActive in sync)
        sessionRepository.findByFacultyIdAndIsActiveTrue(facultyId)
                .forEach(s -> { s.endSession(); sessionRepository.save(s); });

        // Build and save the new session
        AttendanceSession newSession = AttendanceSession.builder()
                .faculty(faculty)
                .scheduledFaculty(scheduledFaculty)
                .section(primarySection)
                .subject(subject)
                .sessionType(sessionType)
                .status(SessionStatus.ACTIVE)
                .isActive(true)
                .latitude(latitude)
                .longitude(longitude)
                .radiusInMeters(radiusInMeters != null ? radiusInMeters : 50.0)
                .secretSeed(UUID.randomUUID().toString())
                .startTime(LocalDateTime.now())
                .durationInMinutes(sessionDuration)
                .build();

        AttendanceSession saved = sessionRepository.save(newSession);

        // ── Wire up AttendanceSessionSection rows ─────────────────────────────
        // Primary section row (isPrimary = true)
        sessionSectionRepository.save(AttendanceSessionSection.builder()
                .session(saved)
                .section(primarySection)
                .isPrimary(true)
                .build());

        // Merged sections (isPrimary = false)
        if (isMerged) {
            for (Long mergedId : mergedSectionIds) {
                if (mergedId.equals(sectionId)) continue; // skip duplicate of primary
                Section mergedSection = sectionRepository.findById(mergedId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Merged section not found: " + mergedId));
                sessionSectionRepository.save(AttendanceSessionSection.builder()
                        .session(saved)
                        .section(mergedSection)
                        .isPrimary(false)
                        .build());
            }
        }

        // Broadcast SessionStartedEvent to each section's websocket topic
        try {
            List<AttendanceSessionSection> activeSections = sessionSectionRepository.findBySessionId(saved.getId());
            for (AttendanceSessionSection ass : activeSections) {
                Long targetSectionId = ass.getSection().getId();
                SessionStartedEvent startedEvent = new SessionStartedEvent(
                        saved.getId(),
                        saved.getSubject() != null ? saved.getSubject().getName() : "General Class",
                        saved.getSubject() != null ? saved.getSubject().getCode() : "N/A",
                        ass.getSection().getSectionName(),
                        saved.getSessionType().name(),
                        saved.getStartTime()
                );
                messagingTemplate.convertAndSend("/topic/session/" + targetSectionId, startedEvent);

                // Create persistent section announcement for offline/history retrieval
                try {
                    Announcements attendanceAnnouncement = new Announcements();
                    attendanceAnnouncement.setTitle("Attendance Session Started: " + (saved.getSubject() != null ? saved.getSubject().getName() : "General Class"));
                    attendanceAnnouncement.setDescription(String.format("Attendance marking is active for %s. Code: active", 
                            saved.getSubject() != null ? saved.getSubject().getName() : "General Class"));
                    attendanceAnnouncement.setAuthor(faculty.getUser() != null ? faculty.getUser().getName() : "Faculty");
                    attendanceAnnouncement.setTime(saved.getStartTime().toString());
                    attendanceAnnouncement.setType("ATTENDANCE_SESSION");
                    attendanceAnnouncement.setSectionId(targetSectionId);
                    attendanceAnnouncement.setCompleted(false);

                    announcementRepository.save(attendanceAnnouncement);

                    // Broadcast to notifications channel
                    messagingTemplate.convertAndSend("/topic/notifications/section/" + targetSectionId, attendanceAnnouncement);
                } catch (Exception ex) {
                    System.err.println("Failed to create attendance session announcement: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast SessionStartedEvent: " + e.getMessage());
        }

        return saved;
    }

    // ── Get Current Code ───────────────────────────────────────────────────────
    public String getCurrentCodeForSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findByIdAndIsActiveTrue(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not active or not found"));
        return generateCodeForTime(session.getSecretSeed(), System.currentTimeMillis());
    }

    // ── End Session ────────────────────────────────────────────────────────────
    @Transactional
    public void endSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.endSession();   // sets status=ENDED, isActive=false, endTime=now
        AttendanceSession saved = sessionRepository.save(session);

        // Broadcast SessionEndedEvent to each section's websocket topic
        try {
            List<AttendanceSessionSection> activeSections = sessionSectionRepository.findBySessionId(saved.getId());
            SessionEndedEvent endedEvent = new SessionEndedEvent(saved.getId());
            for (AttendanceSessionSection ass : activeSections) {
                Long targetSectionId = ass.getSection().getId();
                messagingTemplate.convertAndSend("/topic/session/" + targetSectionId, endedEvent);
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast SessionEndedEvent: " + e.getMessage());
        }

        // Low attendance check
        if (saved.getSubject() != null) {
            try {
                Long subjectId = saved.getSubject().getId();
                Long fId = saved.getFaculty().getId();
                List<AttendanceSessionSection> activeSections = sessionSectionRepository.findBySessionId(saved.getId());
                for (AttendanceSessionSection ass : activeSections) {
                    Long sectionId = ass.getSection().getId();
                    List<Student> students = studentRepository.findBySectionId(sectionId);
                    
                    // Denominator classes conducted for this section & subject
                    long taggedSessions = sessionRepository.countBySubjectIdAndSectionId(subjectId, sectionId);
                    long untaggedSessions = sessionRepository.countUntaggedByFacultyIdAndSectionId(fId, sectionId);
                    long totalConducted = taggedSessions + untaggedSessions;

                    if (totalConducted > 0) {
                        for (Student student : students) {
                            Long studentId = student.getId();
                            long taggedAttended = attendanceRecordRepository.countByStudentIdAndSubjectIdAndSectionId(studentId, subjectId, sectionId);
                            long untaggedAttended = attendanceRecordRepository.countUntaggedByStudentIdAndFacultyIdAndSectionId(studentId, fId, sectionId);
                            long attended = taggedAttended + untaggedAttended;

                            double percentage = Math.round((attended * 100.0 / totalConducted) * 100.0) / 100.0;
                            if (percentage < 75.0) {
                                // Save and broadcast personalized low-attendance warning
                                Announcements warning = new Announcements();
                                warning.setTitle("Low Attendance Warning: " + saved.getSubject().getName());
                                warning.setDescription(String.format("Your attendance in %s is %.2f%%, which is below the required 75%% threshold. (Attended: %d/%d classes)", 
                                        saved.getSubject().getName(), percentage, attended, totalConducted));
                                warning.setAuthor("System");
                                warning.setTime(java.time.LocalDateTime.now().toString());
                                warning.setType("ATTENDANCE_WARNING");
                                warning.setStudentCollegeId(student.getCollegeId());
                                warning.setSectionId(sectionId);
                                warning.setCompleted(false);

                                announcementRepository.save(warning);

                                // Broadcast to the specific student WebSocket destination
                                messagingTemplate.convertAndSend("/topic/notifications/student/" + student.getCollegeId(), warning);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to perform low attendance warning checks: " + e.getMessage());
            }
        }
    }

    // ── Find Active Session for Student (merged-session aware) ─────────────────
    @Transactional
    public Optional<AttendanceSession> findActiveSessionForStudent(String collegeId) {
        Student student = studentRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getSection() == null) return Optional.empty();

        Optional<AttendanceSession> sessionOpt = sessionRepository.findActiveSessionForSection(student.getSection().getId());
        if (sessionOpt.isPresent()) {
            AttendanceSession session = sessionOpt.get();
            // Lazy TTL expiration check
            int duration = session.getDurationInMinutes() != null ? session.getDurationInMinutes() : 40;
            if (session.getStartTime().isBefore(LocalDateTime.now().minusMinutes(duration))) {
                session.endSession();
                sessionRepository.save(session);
                return Optional.empty();
            }
            return Optional.of(session);
        }
        return Optional.empty();
    }

    // ── Find Active Session for Faculty (with Lazy TTL cleanup) ────────────────
    @Transactional
    public Optional<AttendanceSession> findActiveSessionForFaculty(String collegeId) {
        Faculty faculty = facultyRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        List<AttendanceSession> activeSessions = sessionRepository.findByFacultyIdAndIsActiveTrue(faculty.getId());
        AttendanceSession validSession = null;
        
        for (AttendanceSession session : activeSessions) {
            // Lazy TTL expiration check
            int duration = session.getDurationInMinutes() != null ? session.getDurationInMinutes() : 40;
            if (session.getStartTime().isBefore(LocalDateTime.now().minusMinutes(duration))) {
                session.endSession();
                sessionRepository.save(session);
            } else {
                validSession = session;
            }
        }
        
        return Optional.ofNullable(validSession);
    }

    // ── Mark Attendance ────────────────────────────────────────────────────────
    @Transactional
    public void markAttendance(Long sessionId, Long studentId,
                                String submittedCode,
                                Double latitude, Double longitude) {
        AttendanceSession session = sessionRepository.findByIdAndIsActiveTrue(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not active or not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (attendanceRecordRepository.existsByStudentIdAndAttendanceSessionId(studentId, sessionId)) {
            throw new RuntimeException("Attendance already marked for this session");
        }

        // Location validation
        if (session.getLatitude() != null && session.getLongitude() != null) {
            if (latitude == null || longitude == null) {
                throw new RuntimeException("Location data is required for this session");
            }
            double distance = calculateHaversineDistance(
                    session.getLatitude(), session.getLongitude(), latitude, longitude);
            if (distance > session.getRadiusInMeters()) {
                throw new RuntimeException("You are too far from the classroom ("
                        + Math.round(distance) + " meters away). Max allowed: "
                        + session.getRadiusInMeters() + "m");
            }
        }

        // Code validation (current + previous 30-second window)
        long now = System.currentTimeMillis();
        String currentCode  = generateCodeForTime(session.getSecretSeed(), now);
        String previousCode = generateCodeForTime(session.getSecretSeed(), now - 30000);

        if (!submittedCode.equals(currentCode) && !submittedCode.equals(previousCode)) {
            throw new RuntimeException("Code is invalid or has expired.");
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .attendanceSession(session)
                .student(student)
                .markedAt(LocalDateTime.now())
                .markedLatitude(latitude)
                .markedLongitude(longitude)
                .markedBy(MarkSource.STUDENT_TOTP)
                .build();

        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        // Broadcast RosterUpdateEvent to the faculty roster websocket topic
        try {
            RosterUpdateEvent rosterEvent = new RosterUpdateEvent(
                    session.getId(),
                    student.getId(),
                    student.getName(),
                    student.getCollegeId(),
                    student.getRollNumber(),
                    savedRecord.getMarkedAt(),
                    savedRecord.getMarkedBy().name()
            );
            messagingTemplate.convertAndSend("/topic/roster/" + session.getId(), rosterEvent);
        } catch (Exception e) {
            System.err.println("Failed to broadcast RosterUpdateEvent: " + e.getMessage());
        }
    }

    // ── Faculty: get records for a session ────────────────────────────────────
    public List<StudentAttendanceDTO> getRecordsForSession(Long sessionId) {
        return attendanceRecordRepository.findByAttendanceSessionId(sessionId)
                .stream()
                .map(record -> new StudentAttendanceDTO(
                        record.getStudent().getName(),
                        record.getStudent().getCollegeId(),
                        record.getStudent().getRollNumber(),
                        record.getMarkedAt()))
                .collect(Collectors.toList());
    }

    // ── Faculty: manually mark students present ───────────────────────────────
    /**
     * Allows faculty to manually mark students present. Works during:
     * 1. ACTIVE session (always allowed)
     * 2. ENDED session within the grace window (default 15 minutes after end)
     * 3. Any ENDED session if forceOverride = true (faculty flexibility)
     *
     * Each student marked gets MarkSource.FACULTY_MANUAL + the faculty's ID
     * recorded for audit purposes.
     */
    @Transactional
    public List<StudentAttendanceDTO> manualMarkAttendance(
            Long sessionId, Long facultyId, List<String> studentCollegeIds,
            String reason, boolean forceOverride) {

        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        // Verify this faculty owns the session
        if (!session.getFaculty().getId().equals(facultyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manually mark students for your own sessions.");
        }

        // Check if manual marking is allowed
        if (!forceOverride && !session.isManualMarkAllowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Manual marking window has closed (" + session.getManualMarkGraceMinutes()
                    + " minutes after session end). Use force override if needed.");
        }

        // Even with forceOverride, CANCELLED sessions cannot be marked
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot mark attendance for a cancelled session.");
        }

        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Faculty not found"));

        List<StudentAttendanceDTO> markedStudents = new java.util.ArrayList<>();

        for (String collegeId : studentCollegeIds) {
            Student student = studentRepository.findByCollegeId(collegeId)
                    .orElse(null);
            if (student == null) continue; // Skip unknown college IDs silently

            // Skip if already marked
            if (attendanceRecordRepository.existsByStudentIdAndAttendanceSessionId(
                    student.getId(), sessionId)) {
                continue;
            }

            AttendanceRecord record = AttendanceRecord.builder()
                    .attendanceSession(session)
                    .student(student)
                    .markedAt(LocalDateTime.now())
                    .markedBy(MarkSource.FACULTY_MANUAL)
                    .markedByFaculty(faculty)
                    .graceReason(reason)
                    .build();

            AttendanceRecord saved = attendanceRecordRepository.save(record);

            markedStudents.add(new StudentAttendanceDTO(
                    student.getName(), student.getCollegeId(), student.getRollNumber(), saved.getMarkedAt()));

            // Broadcast roster update via WebSocket
            try {
                RosterUpdateEvent rosterEvent = new RosterUpdateEvent(
                        session.getId(), student.getId(), student.getName(),
                        student.getCollegeId(), student.getRollNumber(), saved.getMarkedAt(),
                        MarkSource.FACULTY_MANUAL.name());
                messagingTemplate.convertAndSend(
                        "/topic/roster/" + session.getId(), rosterEvent);
            } catch (Exception e) {
                System.err.println("Failed to broadcast manual mark RosterUpdateEvent: " + e.getMessage());
            }
        }

        return markedStudents;
    }

    // ── Faculty: get absent students for a session ────────────────────────────
    /**
     * Returns all students in the session's sections who have NOT yet
     * marked attendance. Works for both REGULAR and MERGED sessions.
     */
    public List<Map<String, Object>> getAbsentStudentsForSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        // Collect all section IDs (primary + merged)
        List<Long> sectionIds = new java.util.ArrayList<>();
        sectionIds.add(session.getSection().getId());
        List<AttendanceSessionSection> sessionSections =
                sessionSectionRepository.findBySessionId(sessionId);
        for (AttendanceSessionSection ass : sessionSections) {
            Long secId = ass.getSection().getId();
            if (!sectionIds.contains(secId)) {
                sectionIds.add(secId);
            }
        }

        // Get all students in those sections
        List<Student> allStudents = new java.util.ArrayList<>();
        for (Long secId : sectionIds) {
            allStudents.addAll(studentRepository.findBySectionId(secId));
        }

        // Get IDs of students who already marked
        java.util.Set<Long> markedStudentIds = attendanceRecordRepository
                .findByAttendanceSessionId(sessionId)
                .stream()
                .map(r -> r.getStudent().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Filter to absent only
        return allStudents.stream()
                .filter(s -> !markedStudentIds.contains(s.getId()))
                .map(s -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("collegeId", s.getCollegeId());
                    m.put("name", s.getName());
                    m.put("rollNumber", s.getRollNumber());
                    m.put("sectionName", s.getSection() != null ? s.getSection().getSectionName() : "N/A");
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Faculty: get session history (recent sessions) ────────────────────────
    public List<Map<String, Object>> getFacultySessionHistory(Long facultyId) {
        return sessionRepository.findByFacultyIdOrderByStartTimeDesc(facultyId)
                .stream()
                .limit(20) // Last 20 sessions
                .map(s -> {
                    long presentCount = attendanceRecordRepository
                            .findByAttendanceSessionId(s.getId()).size();
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("sessionId", s.getId());
                    m.put("sectionName", s.getSection().getSectionName());
                    m.put("subjectName", s.getSubject() != null ? s.getSubject().getName() : "General");
                    m.put("subjectCode", s.getSubject() != null ? s.getSubject().getCode() : "N/A");
                    m.put("sessionType", s.getSessionType().name());
                    m.put("status", s.getStatus().name());
                    m.put("startTime", s.getStartTime().toString());
                    m.put("endTime", s.getEndTime() != null ? s.getEndTime().toString() : null);
                    m.put("presentCount", presentCount);
                    m.put("manualMarkAllowed", s.isManualMarkAllowed());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Student: per-subject attendance summary ────────────────────────────────
    public List<AttendanceSummaryDTO> getMyAttendanceSummary(String collegeId) {
        Student student = studentRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getSection() == null) return List.of();
        Long sectionId = student.getSection().getId();
        Long studentId = student.getId();

        // Fetch all subject assignments for this student's section (deduplicated by subjectId)
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findAll().stream()
                .filter(a -> a.getSection() != null && a.getSection().getId().equals(sectionId))
                // Remove duplicate subject entries (same subject assigned multiple times)
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getSubject().getId(),
                        a -> a,
                        (a1, a2) -> a1))
                .values().stream().toList();

        return assignments.stream().map(assignment -> {
            Long subjectId = assignment.getSubject().getId();
            Long fId = assignment.getFaculty().getId();

            // DENOMINATOR: sessions tagged with this exact subject for this section
            long taggedSessions = sessionRepository.countBySubjectIdAndSectionId(subjectId, sectionId);

            // FALLBACK: old sessions by this faculty with no subject tag (legacy data)
            long untaggedSessions = sessionRepository
                    .countUntaggedByFacultyIdAndSectionId(fId, sectionId);

            long totalConducted = taggedSessions + untaggedSessions;

            // NUMERATOR attended for tagged sessions
            long taggedAttended = attendanceRecordRepository
                    .countByStudentIdAndSubjectIdAndSectionId(studentId, subjectId, sectionId);

            // NUMERATOR attended for legacy untagged sessions
            long untaggedAttended = attendanceRecordRepository
                    .countUntaggedByStudentIdAndFacultyIdAndSectionId(studentId, fId, sectionId);

            long attended = taggedAttended + untaggedAttended;

            double percentage = totalConducted > 0
                    ? Math.round((attended * 100.0 / totalConducted) * 100.0) / 100.0
                    : 0.0;

            String status = percentage >= 75 ? "SAFE"
                          : percentage >= 60 ? "WARNING"
                          : "DANGER";

            return AttendanceSummaryDTO.builder()
                    .subjectId(subjectId)
                    .subjectName(assignment.getSubject().getName())
                    .subjectCode(assignment.getSubject().getCode())
                    .totalClasses((int) totalConducted)
                    .attended((int) attended)
                    .percentage(percentage)
                    .status(status)
                    .build();
        }).toList();
    }

    // ── Utility: TOTP-style code generation ───────────────────────────────────
    public static String generateCodeForTime(String seed, long timeMillis) {
        long timeWindow = timeMillis / 30000; // 30 second rotation instead of 15
        String input = seed + timeWindow;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            int offset = hash.length - 4;
            int binary = ((hash[offset]     & 0x7f) << 24)
                       | ((hash[offset + 1] & 0xff) << 16)
                       | ((hash[offset + 2] & 0xff) << 8)
                       |  (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Code generation failed", e);
        }
    }

    // ── Utility: Haversine distance ────────────────────────────────────────────
    public static double calculateHaversineDistance(double lat1, double lon1,
                                                     double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // metres
    }
}
