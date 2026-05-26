package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AttendanceMarkRequestDto;
import com.ucmp.ucmp_backend.dto.AttendanceStartRequestDto;
import com.ucmp.ucmp_backend.dto.ManualMarkRequestDto;
import com.ucmp.ucmp_backend.dto.StudentAttendanceDTO;
import com.ucmp.ucmp_backend.model.AttendanceSession;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.repository.AttendanceRecordRepository;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    // ── Faculty: start a session ───────────────────────────────────────────────
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> startSession(
            Authentication authentication,
            @RequestBody AttendanceStartRequestDto request) {
        String collegeId = authentication.getName();
        Faculty faculty = facultyRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not a Faculty"));

        AttendanceSession session = attendanceService.startSession(
                faculty.getId(),
                request.getSectionId(),
                request.getSubjectId(),
                request.getMergedSectionIds(),    // null for REGULAR, list for MERGED
                request.getScheduledFacultyId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusInMeters(),
                request.getDurationInMinutes());
        return ResponseEntity.ok(Map.of("id", session.getId(), "sessionType", session.getSessionType()));
    }

    // ── Get current rotating code for a session ────────────────────────────────
    @GetMapping("/session/{sessionId}/code")
    public ResponseEntity<String> getCurrentCode(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getCurrentCodeForSession(sessionId));
    }

    // ── Student: mark attendance ───────────────────────────────────────────────
    @PostMapping("/mark")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<String> markAttendance(
            Authentication authentication,
            @RequestBody AttendanceMarkRequestDto request) {
        try {
            String collegeId = authentication.getName();
            Student student = studentRepository.findByCollegeId(collegeId)
                    .orElseThrow(() -> new RuntimeException("Logged in user is not a Student"));

            attendanceService.markAttendance(
                    request.getSessionId(),
                    student.getId(),
                    request.getCode(),
                    request.getLatitude(),
                    request.getLongitude());
            return ResponseEntity.ok("Attendance marked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Student / Faculty: find active session ─────────────────────────
    @GetMapping("/active-session")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getActiveSession(Authentication authentication) {
        String collegeId = authentication.getName();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("STUDENT"));

        if (isStudent) {
            return attendanceService.findActiveSessionForStudent(collegeId)
                    .map(session -> ResponseEntity.ok(Map.of(
                            "id", session.getId(),
                            "sectionId", session.getSection().getId(),
                            "sectionName", session.getSection().getSectionName(),
                            "subjectId", session.getSubject() != null ? session.getSubject().getId() : null,
                            "subjectName", session.getSubject() != null ? session.getSubject().getName() : "General Class",
                            "subjectCode", session.getSubject() != null ? session.getSubject().getCode() : "N/A"
                    )))
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return attendanceService.findActiveSessionForFaculty(collegeId)
                    .map(session -> ResponseEntity.ok(Map.of(
                            "id", session.getId(),
                            "sectionId", session.getSection().getId(),
                            "sectionName", session.getSection().getSectionName(),
                            "subjectId", session.getSubject() != null ? session.getSubject().getId() : null,
                            "subjectName", session.getSubject() != null ? session.getSubject().getName() : "General Class",
                            "subjectCode", session.getSubject() != null ? session.getSubject().getCode() : "N/A"
                    )))
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    // ── Faculty: get all records for a session ─────────────────────────────────
    @GetMapping("/session/{sessionId}/records")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<List<StudentAttendanceDTO>> getSessionRecords(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getRecordsForSession(sessionId));
    }

    // ── End a session ──────────────────────────────────────────────────────────
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<String> endSession(@PathVariable Long sessionId) {
        attendanceService.endSession(sessionId);
        return ResponseEntity.ok("Session ended");
    }

    // ── Faculty: manually mark students present ────────────────────────────────
    /**
     * POST /api/attendance/session/{sessionId}/manual-mark
     * Body: { "studentCollegeIds": ["22BCS001", "22BCS015"], "reason": "Dead phones" }
     * Query param: forceOverride=true to mark even after grace window
     *
     * Works during ACTIVE sessions, within grace window after ENDED,
     * and with forceOverride=true for any ENDED session (faculty flexibility).
     */
    @PostMapping("/session/{sessionId}/manual-mark")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> manualMarkAttendance(
            Authentication authentication,
            @PathVariable Long sessionId,
            @RequestBody ManualMarkRequestDto request,
            @RequestParam(defaultValue = "false") boolean forceOverride) {
        try {
            String collegeId = authentication.getName();
            Faculty faculty = facultyRepository.findByCollegeId(collegeId)
                    .orElseThrow(() -> new RuntimeException("Logged in user is not a Faculty"));

            List<StudentAttendanceDTO> marked = attendanceService.manualMarkAttendance(
                    sessionId, faculty.getId(),
                    request.getStudentCollegeIds(),
                    request.getReason(),
                    forceOverride);

            return ResponseEntity.ok(Map.of(
                    "message", marked.size() + " student(s) marked present",
                    "markedStudents", marked));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Faculty: get absent students for a session ─────────────────────────────
    /**
     * GET /api/attendance/session/{sessionId}/absent-students
     * Returns students who haven't marked attendance yet (for manual mark panel).
     */
    @GetMapping("/session/{sessionId}/absent-students")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> getAbsentStudents(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getAbsentStudentsForSession(sessionId));
    }

    // ── Faculty: session history (for post-session manual marking) ─────────────
    /**
     * GET /api/attendance/faculty/session-history
     * Returns the faculty's recent sessions with present count and
     * whether manual marking is still allowed.
     */
    @GetMapping("/faculty/session-history")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> getFacultySessionHistory(
            Authentication authentication,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String collegeId = authentication.getName();
        Faculty faculty = facultyRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not a Faculty"));

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;

        if (startDate != null && !startDate.trim().isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        return ResponseEntity.ok(attendanceService.getFacultySessionHistory(faculty.getId(), start, end));
    }

    @GetMapping("/faculty/debarred-list")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> getDebarredList(
            Authentication authentication,
            @RequestParam Long subjectId,
            @RequestParam Long sectionId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String collegeId = authentication.getName();
        Faculty faculty = facultyRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not a Faculty"));

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;

        if (startDate != null && !startDate.trim().isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        return ResponseEntity.ok(attendanceService.getDebarredList(
                faculty.getId(), subjectId, sectionId, start, end));
    }

    // ── Student: per-subject attendance summary ────────────────────────────────
    @GetMapping("/my-summary")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<?> getMyAttendanceSummary(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getMyAttendanceSummary(authentication.getName()));
    }

    // ── Student: recent attendance history log ─────────────────────────────────
    @GetMapping("/my-history")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<?> getMyAttendanceHistory(Authentication authentication) {
        String collegeId = authentication.getName();
        Student student = studentRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Map<String, Object>> result = attendanceRecordRepository
                .findByStudentIdOrderByMarkedAtDesc(student.getId())
                .stream()
                .map(r -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("sessionId", r.getAttendanceSession().getId());
                    m.put("sectionName", r.getAttendanceSession().getSection().getSectionName());
                    m.put("subjectName", r.getAttendanceSession().getSubject() != null
                            ? r.getAttendanceSession().getSubject().getName() : "General");
                    m.put("markedAt", r.getMarkedAt().toString());
                    m.put("markedBy", r.getMarkedBy().name());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
