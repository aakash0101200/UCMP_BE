package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Section;
import com.ucmp.ucmp_backend.dto.SectionDTO;
import com.ucmp.ucmp_backend.repository.SectionRepository;
import com.ucmp.ucmp_backend.repository.BatchRepository;
import com.ucmp.ucmp_backend.validator.AdminScopeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionRepository sectionRepository;
    private final BatchRepository batchRepository;
    private final AdminScopeValidator adminScopeValidator;

    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        List<SectionDTO> sections = sectionRepository.findAll().stream()
                .map(s -> new SectionDTO(s.getId(), s.getSectionName(), s.getYear(), s.getBatch() != null ? s.getBatch().getId() : null))
                .toList();
        return ResponseEntity.ok(sections);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSection(
            Authentication authentication,
            @RequestBody SectionCreationDTO dto) {
        
        com.ucmp.ucmp_backend.model.Batch batch = batchRepository.findById(dto.getBatchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch not found: " + dto.getBatchId()));

        try {
            adminScopeValidator.enforceAccess(authentication, batch.getBatchName(), dto.getYear());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }

        Section section = new Section();
        section.setSectionName(dto.getSectionName());
        section.setYear(dto.getYear());
        section.setBatch(batch);

        Section saved = sectionRepository.save(section);

        return ResponseEntity.ok(new SectionDTO(saved.getId(), saved.getSectionName(), saved.getYear(), saved.getBatch().getId()));
    }

    @lombok.Data
    public static class SectionCreationDTO {
        private String sectionName;
        private Integer year;
        private Long batchId;
    }
}
