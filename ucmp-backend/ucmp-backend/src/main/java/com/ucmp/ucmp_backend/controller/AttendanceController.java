package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AttendanceMarkRequestDto;
import com.ucmp.ucmp_backend.dto.AttendanceStartRequestDto;
import com.ucmp.ucmp_backend.dto.StudentAttendanceDTO;
import com.ucmp.ucmp_backend.model.AttendanceSession;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('FACULTY')") // This is the standard RBAC approach
    public ResponseEntity<?> startSession(
            Authentication authentication,
            @RequestBody AttendanceStartRequestDto request) {
        String collegeId = authentication.getName();
        Faculty faculty = facultyRepository.findByCollegeId(collegeId)
            .orElseThrow(() -> new RuntimeException("Logged in user is not a Faculty"));

        AttendanceSession session = attendanceService.startSession(
            faculty.getId(), 
            request.getSectionId(), 
            request.getLatitude(), 
            request.getLongitude(), 
            request.getRadiusInMeters()
        );
        return ResponseEntity.ok(Map.of("id", session.getId()));
    }

    @GetMapping("/session/{sessionId}/code")
    public ResponseEntity<String> getCurrentCode(@PathVariable Long sessionId) {
        String code = attendanceService.getCurrentCodeForSession(sessionId);
        return ResponseEntity.ok(code);
    }

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
                request.getLongitude()
            );
            return ResponseEntity.ok("Attendance marked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/active-session")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<?> getActiveSessionForStudent(Authentication authentication) {
        String collegeId = authentication.getName();

        // This logic finds the student, gets their section,
        // and looks for an active session for that section.
        return attendanceService.findActiveSessionForStudent(collegeId)
                .map(session -> ResponseEntity.ok(Map.of(
                        "id", session.getId(),
                        "sectionName", session.getSection().getSectionName()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/session/{sessionId}/records")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<List<StudentAttendanceDTO>> getSessionRecords(@PathVariable Long sessionId) {
        List<StudentAttendanceDTO> records = attendanceService.getRecordsForSession(sessionId);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<String> endSession(@PathVariable Long sessionId) {
        attendanceService.endSession(sessionId);
        return ResponseEntity.ok("Session ended");
    }
}
