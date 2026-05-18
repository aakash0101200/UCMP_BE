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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // ── Start Session ──────────────────────────────────────────────────────────
    /**
     * @param mergedSectionIds  Additional section IDs to include (for MERGED sessions).
     *                          null or empty = REGULAR session.
     *                          The primary sectionId is always included automatically.
     */
    @Transactional
    public AttendanceSession startSession(Long facultyId, Long sectionId, Long subjectId,
                                          List<Long> mergedSectionIds,
                                          Double latitude, Double longitude,
                                          Double radiusInMeters) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        Section primarySection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        // Resolve subject — nullable for backward-compat
        Subject subject = (subjectId != null)
                ? subjectRepository.findById(subjectId).orElse(null)
                : null;

        // Determine session type
        boolean isMerged = mergedSectionIds != null && !mergedSectionIds.isEmpty();
        SessionType sessionType = isMerged ? SessionType.MERGED : SessionType.REGULAR;

        // End any existing active sessions for this faculty (keep isActive in sync)
        sessionRepository.findByFacultyIdAndIsActiveTrue(facultyId)
                .forEach(s -> { s.endSession(); sessionRepository.save(s); });

        // Build and save the new session
        AttendanceSession newSession = AttendanceSession.builder()
                .faculty(faculty)
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
        sessionRepository.save(session);
    }

    // ── Find Active Session for Student (merged-session aware) ─────────────────
    public Optional<AttendanceSession> findActiveSessionForStudent(String collegeId) {
        Student student = studentRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getSection() == null) return Optional.empty();

        // findActiveSessionForSection checks BOTH primary section field AND the
        // AttendanceSessionSection join table — so merged sessions are found too.
        return sessionRepository.findActiveSessionForSection(student.getSection().getId());
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
                .build();

        attendanceRecordRepository.save(record);
    }

    // ── Faculty: get records for a session ────────────────────────────────────
    public List<StudentAttendanceDTO> getRecordsForSession(Long sessionId) {
        return attendanceRecordRepository.findByAttendanceSessionId(sessionId)
                .stream()
                .map(record -> new StudentAttendanceDTO(
                        record.getStudent().getName(),
                        record.getStudent().getCollegeId(),
                        record.getMarkedAt()))
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
            Long facultyId = assignment.getFaculty().getId();

            // DENOMINATOR: sessions tagged with this exact subject for this section
            long taggedSessions = sessionRepository.countBySubjectIdAndSectionId(subjectId, sectionId);

            // FALLBACK: old sessions by this faculty with no subject tag (legacy data)
            long untaggedSessions = sessionRepository
                    .countUntaggedByFacultyIdAndSectionId(facultyId, sectionId);

            long totalConducted = taggedSessions + untaggedSessions;

            // NUMERATOR attended for tagged sessions
            long taggedAttended = attendanceRecordRepository
                    .countByStudentIdAndSubjectIdAndSectionId(studentId, subjectId, sectionId);

            // NUMERATOR attended for legacy untagged sessions
            long untaggedAttended = attendanceRecordRepository
                    .countUntaggedByStudentIdAndFacultyIdAndSectionId(studentId, facultyId, sectionId);

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
