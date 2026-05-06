package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.*;
import com.ucmp.ucmp_backend.model.SubjectAssignment;
import com.ucmp.ucmp_backend.service.TimetableService;
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
     * Returns: { hasConflicts: true/false, conflicts: ["..."] }
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ConflictCheckResult> validateEntry(
            @Valid @RequestBody CreateTimetableEntryRequest request) {
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
    public ResponseEntity<?> createEntry(@Valid @RequestBody CreateTimetableEntryRequest request) {
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
            @PathVariable Long id,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
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
    public ResponseEntity<String> deleteEntry(@PathVariable Long id) {
        timetableService.deleteEntry(id);
        return ResponseEntity.ok("Entry deleted successfully");
    }

    // ─── Subject Assignment endpoints ──────────────────────────────────────

    /**
     * POST /api/timetable/assignment
     * Create a subject assignment (admin plans who teaches what).
     */
    @PostMapping("/assignment")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createAssignment(@Valid @RequestBody SubjectAssignmentRequest request) {
        try {
            SubjectAssignment result = timetableService.createAssignment(request);
            return ResponseEntity.ok(result);
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
    public ResponseEntity<List<SubjectAssignment>> getAssignments(@RequestParam String term) {
        return ResponseEntity.ok(timetableService.getAssignmentsForTerm(term));
    }

    /**
     * GET /api/timetable/assignment/section/{id}?term=2026-27-ODD
     */
    @GetMapping("/assignment/section/{sectionId}")
    public ResponseEntity<List<SubjectAssignment>> getAssignmentsForSection(
            @PathVariable Long sectionId,
            @RequestParam String term) {
        return ResponseEntity.ok(timetableService.getAssignmentsForSection(sectionId, term));
    }

    /**
     * DELETE /api/timetable/assignment/{id}
     */
    @DeleteMapping("/assignment/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteAssignment(@PathVariable Long id) {
        timetableService.deleteAssignment(id);
        return ResponseEntity.ok("Assignment deleted");
    }
}
