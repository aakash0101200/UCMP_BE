package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Section;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.dto.SectionDTO;
import com.ucmp.ucmp_backend.repository.SectionRepository;
import com.ucmp.ucmp_backend.repository.BatchRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.validator.AdminScopeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionRepository sectionRepository;
    private final BatchRepository batchRepository;
    private final AdminScopeValidator adminScopeValidator;
    private final StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        List<SectionDTO> sections = sectionRepository.findAll().stream()
                .map(s -> new SectionDTO(s.getId(), s.getSectionName(), s.getYear(),
                        s.getBatch() != null ? s.getBatch().getId() : null,
                        s.getBatch() != null ? s.getBatch().getBatchName() : null))
                .toList();
        return ResponseEntity.ok(sections);
    }


    // ─── Quick-Connect: List students in a section (lightweight) ─────────────
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudentsInSection(@PathVariable Long id) {
        if (!sectionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, String>> students = studentRepository.findBySectionId(id).stream()
                .map(s -> Map.of(
                        "collegeId", s.getCollegeId() != null ? s.getCollegeId() : "",
                        "name", s.getName() != null ? s.getName() : "",
                        "rollNumber", s.getRollNumber() != null ? s.getRollNumber() : ""))
                .toList();
        return ResponseEntity.ok(students);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSection(
            Authentication authentication,
            @RequestBody SectionCreationDTO dto) {

        if (dto.getSectionName() == null || dto.getSectionName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section name is required.");
        }
        if (dto.getYear() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year is required.");
        }

        com.ucmp.ucmp_backend.model.Batch batch = batchRepository.findById(dto.getBatchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Batch not found: " + dto.getBatchId()));

        try {
            adminScopeValidator.enforceAccess(authentication, batch.getBatchName(), dto.getYear());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }

        Optional<Section> existing = sectionRepository.findBySectionNameIgnoreCaseAndBatchIdAndYear(
                dto.getSectionName().trim(), batch.getId(), dto.getYear());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Section with name '" + dto.getSectionName().trim() + "' already exists for this batch and year.");
        }

        Section section = new Section();
        section.setSectionName(dto.getSectionName().trim());
        section.setYear(dto.getYear());
        section.setBatch(batch);

        Section saved = sectionRepository.save(section);

        return ResponseEntity
                .ok(new SectionDTO(saved.getId(), saved.getSectionName(), saved.getYear(), saved.getBatch().getId(), saved.getBatch().getBatchName()));
    }


    @lombok.Data
    public static class SectionCreationDTO {
        private String sectionName;
        private Integer year;
        private Long batchId;
    }
}
