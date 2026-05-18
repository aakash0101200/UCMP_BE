package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Section;
import com.ucmp.ucmp_backend.dto.SectionDTO;
import com.ucmp.ucmp_backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionRepository sectionRepository;

    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        List<SectionDTO> sections = sectionRepository.findAll().stream()
                .map(s -> new SectionDTO(s.getId(), s.getSectionName()))
                .toList();
        return ResponseEntity.ok(sections);
    }
}
