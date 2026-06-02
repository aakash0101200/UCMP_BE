package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import com.ucmp.ucmp_backend.dto.*;
import com.ucmp.ucmp_backend.service.TimetableService;
import com.ucmp.ucmp_backend.validator.AdminScopeValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final com.ucmp.ucmp_backend.service.TimetableResolutionService resolutionService;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TimetableOverrideRepository overrideRepository;
    private final AdminScopeValidator adminScopeValidator;

    private String enforceAdminRoleAndDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));

        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
            return "ALL";
        }
        return adminUser.getDepartment();
    }

    // ─── Student / Faculty READ endpoints ─────────────────────────────────

    /**
     * GET /api/timetable/section/{id}?term=2026-27-ODD
     * Used by the student weekly grid view.
     */
    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<TimetableEntryResponseDTO>> getSectionSchedule(
            @PathVariable Long sectionId,
            @RequestParam String term) {
        return ResponseEntity.ok(timetableService.getScheduleForSection(sectionId, term));
    }

    /**
     * GET /api/timetable/faculty/{id}?term=2026-27-ODD
     * Used by the faculty schedule view.
     */
    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<TimetableEntryResponseDTO>> getFacultySchedule(
            @PathVariable Long facultyId,
            @RequestParam String term) {
        return ResponseEntity.ok(timetableService.getScheduleForFaculty(facultyId, term));
    }

    /**
     * GET /api/timetable/room/{id}?term=2026-27-ODD
     * Used by admin room occupancy view.
     */
    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<TimetableEntryResponseDTO>> getRoomSchedule(
            @PathVariable Long roomId,
            @RequestParam String term) {
        return ResponseEntity.ok(timetableService.getScheduleForRoom(roomId, term));
    }

    // ─── Admin WRITE endpoints ─────────────────────────────────────────────

    /**
     * POST /api/timetable/validate
     * Pre-save conflict check. Call this BEFORE creating an entry.
     * Returns: { hasConflicts: true/false, conflicts: [""..."] }
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ConflictCheckResult> validateEntry(
            Authentication authentication,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        adminScopeValidator.enforceAccessToSection(authentication, request.getSectionId());
        ConflictCheckResult result = timetableService.validateEntry(
                request.getSectionId(), request.getFacultyId(), request.getRoomId(), request.getSubjectId(),
                request.getDay(), request.getStartTime(), request.getEndTime(),
                request.getAcademicTerm(), null);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/timetable/entry
     * Create a new timetable entry. Validates first, rejects if conflicts found.
     */
    @PostMapping("/entry")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createEntry(Authentication authentication,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        adminScopeValidator.enforceAccessToSection(authentication, request.getSectionId());
        try {
            TimetableEntryResponseDTO created = timetableService.createEntry(request);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/timetable/entry/{id}
     * Update an existing entry. Conflict check excludes the entry being updated.
     */
    @PutMapping("/entry/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateEntry(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        adminScopeValidator.enforceAccessToSection(authentication, request.getSectionId());
        try {
            TimetableEntryResponseDTO updated = timetableService.updateEntry(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * DELETE /api/timetable/entry/{id}
     */
    @DeleteMapping("/entry/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteEntry(Authentication authentication, @PathVariable Long id) {
        TimetableEntry entry = timetableEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timetable entry not found"));
        adminScopeValidator.enforceAccessToSection(authentication, entry.getSection().getId());
        timetableService.deleteEntry(id);
        return ResponseEntity.ok("Entry deleted successfully");
    }

    // ─── Subject Assignment endpoints ──────────────────────────────────────

    /**
     * POST /api/timetable/assignment
     * Create a subject assignment (admin plan who teaches what).
     */
    @PostMapping("/assignment")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createAssignment(Authentication authentication,
            @Valid @RequestBody SubjectAssignmentRequest request) {
        adminScopeValidator.enforceAccessToSection(authentication, request.getSectionId());
        try {
            SubjectAssignment result = timetableService.createAssignment(request);
            return ResponseEntity.ok(mapToAssignmentDTO(result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/timetable/assignment?term=2026-27-ODD
     * Get all subject assignments for a term (generator input).
     */
    @GetMapping("/assignment")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SubjectAssignmentDTO>> getAssignments(@RequestParam String term) {
        List<SubjectAssignmentDTO> dtos = timetableService.getAssignmentsForTerm(term).stream()
                .map(this::mapToAssignmentDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/timetable/assignment/section/{id}?term=2026-27-ODD
     */
    @GetMapping("/assignment/section/{sectionId}")
    public ResponseEntity<List<SubjectAssignmentDTO>> getAssignmentsForSection(
            @PathVariable Long sectionId,
            @RequestParam String term) {
        List<SubjectAssignmentDTO> dtos = timetableService.getAssignmentsForSection(sectionId, term).stream()
                .map(this::mapToAssignmentDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private SubjectAssignmentDTO mapToAssignmentDTO(SubjectAssignment a) {
        return SubjectAssignmentDTO.builder()
                .id(a.getId())
                .subjectId(a.getSubject() != null ? a.getSubject().getId() : null)
                .subjectCode(a.getSubject() != null ? a.getSubject().getCode() : null)
                .subjectName(a.getSubject() != null ? a.getSubject().getName() : null)
                .facultyId(a.getFaculty() != null ? a.getFaculty().getId() : null)
                .facultyName(
                        a.getFaculty() != null && a.getFaculty().getUser() != null ? a.getFaculty().getUser().getName()
                                : null)
                .facultyCollegeId(a.getFaculty() != null ? a.getFaculty().getCollegeId() : null)
                .sectionId(a.getSection() != null ? a.getSection().getId() : null)
                .sectionName(a.getSection() != null ? a.getSection().getSectionName() : null)
                .academicTerm(a.getAcademicTerm())
                .weeklySlots(a.getWeeklySlots())
                .googleClassroomLink(a.getGoogleClassroomLink())
                .build();
    }

    /**
     * DELETE /api/timetable/assignment/{id}
     */
    @DeleteMapping("/assignment/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteAssignment(Authentication authentication, @PathVariable Long id) {
        SubjectAssignment assignment = subjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        adminScopeValidator.enforceAccessToSection(authentication, assignment.getSection().getId());
        timetableService.deleteAssignment(id);
        return ResponseEntity.ok("Assignment deleted");
    }

    /**
     * PATCH /api/timetable/assignment/{id}/classroom-link
     * Faculty sets (or updates) the Google Classroom URL for their own assignment.
     * Ownership is validated in the service — a faculty cannot change another's link.
     */
    @PatchMapping("/assignment/{id}/classroom-link")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> setClassroomLink(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String facultyCollegeId = authentication.getName();
        String link = body.get("link");
        try {
            SubjectAssignment updated = timetableService.setClassroomLink(id, facultyCollegeId, link);
            return ResponseEntity.ok(mapToAssignmentDTO(updated));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/timetable/assignment/my?term=2026-27-ODD
     * Faculty-only: returns their own subject assignments (with classroom links).
     * Feeds the "Connect Google Classroom" panel in the Gradebook page.
     */
    @GetMapping("/assignment/my")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<List<SubjectAssignmentDTO>> getMyAssignments(
            Authentication authentication,
            @RequestParam String term) {
        String facultyCollegeId = authentication.getName();
        List<SubjectAssignmentDTO> dtos = timetableService.getAssignmentsForFaculty(facultyCollegeId, term).stream()
                .map(this::mapToAssignmentDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/timetable/entries/{entryId}/cancel
     * Allows faculty to cancel a specific day's class.
     */
    @PostMapping("/entries/{entryId}/cancel")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> cancelClass(
            org.springframework.security.core.Authentication authentication,
            @PathVariable Long entryId,
            @RequestBody ClassCancellationRequestDto request) {
        try {
            String collegeId = authentication.getName();
            com.ucmp.ucmp_backend.model.ClassCancellation cancellation = timetableService.cancelEntry(
                    entryId,
                    request.getCancellationDate(),
                    request.getReason(),
                    collegeId);
            return ResponseEntity.ok(cancellation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // AOCS: Resolved Schedules
    @GetMapping("/section/{sectionId}/resolved")
    public ResponseEntity<List<TimetableEntryResponseDTO>> getResolvedSectionSchedule(
            @PathVariable Long sectionId,
            @RequestParam String date,
            @RequestParam String term) {
        return ResponseEntity
                .ok(resolutionService.getResolvedScheduleForSection(sectionId, java.time.LocalDate.parse(date), term));
    }

    @GetMapping("/faculty/{facultyId}/resolved")
    public ResponseEntity<List<TimetableEntryResponseDTO>> getResolvedFacultySchedule(
            @PathVariable Long facultyId,
            @RequestParam String date,
            @RequestParam String term) {
        return ResponseEntity
                .ok(resolutionService.getResolvedScheduleForFaculty(facultyId, java.time.LocalDate.parse(date), term));
    }

    // AOCS: Live Availability
    @GetMapping("/availability")
    public ResponseEntity<List<FacultyAvailabilityResponse>> getFacultyAvailability(
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Long subjectId,
            @RequestParam String term) {
        return ResponseEntity.ok(timetableService.getFacultyAvailability(
                java.time.LocalDate.parse(date),
                java.time.LocalTime.parse(startTime),
                java.time.LocalTime.parse(endTime),
                subjectId,
                term));
    }

    // AOCS: Overrides CRUD
    @PostMapping("/override")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createOverride(Authentication authentication,
            @RequestBody TimetableOverrideRequestDTO dto) {
        if (dto.getSectionIds() != null && !dto.getSectionIds().isEmpty()) {
            for (Long sId : dto.getSectionIds()) {
                adminScopeValidator.enforceAccessToSection(authentication, sId);
            }
        } else if (dto.getTimetableEntryId() != null) {
            TimetableEntry entry = timetableEntryRepository.findById(dto.getTimetableEntryId())
                    .orElseThrow(() -> new RuntimeException("Timetable entry template not found"));
            adminScopeValidator.enforceAccessToSection(authentication, entry.getSection().getId());
        }
        try {
            return ResponseEntity.ok(timetableService.createOverride(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/override/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> cancelOverride(Authentication authentication, @PathVariable Long id) {
        TimetableOverride override = overrideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Override not found"));
        if (override.getSections() != null) {
            for (Section s : override.getSections()) {
                adminScopeValidator.enforceAccessToSection(authentication, s.getId());
            }
        }
        try {
            return ResponseEntity.ok(timetableService.cancelOverride(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // AOCS: Operational Metrics
    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AocsMetricsResponse> getAocsMetrics(@RequestParam String term) {
        return ResponseEntity.ok(timetableService.getAocsMetrics(term));
    }

    @GetMapping("/terms")
    public ResponseEntity<List<String>> getAcademicTerms() {
        return ResponseEntity.ok(timetableService.getDistinctAcademicTerms());
    }
}
