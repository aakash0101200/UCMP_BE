package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.SectionDTO;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.service.FacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public  FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @PostMapping
    public Faculty createFaculty(@RequestBody Faculty faculty) {
        return facultyService.saveFaculty(faculty);
    }

    @GetMapping
    public List<Faculty> getAllFaculties() {
        return facultyService.getAllFaculties();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Faculty> getFacultyById(@PathVariable Long id) {
        return facultyService.getFacultyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public  ResponseEntity<Void> deleteFacultyById(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }
    // Make sure to import org.springframework.security.core.Authentication;
    @GetMapping("/my-sections")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> getMySections(Authentication authentication) {
        String collegeId = authentication.getName();

        return facultyService.findByCollegeId(collegeId)
                .map(faculty -> {
                    // Map the complex entities to simple, safe objects
                    var sectionList = faculty.getSections().stream()
                            .map(s -> new SectionDTO(s.getId(), s.getSectionName()))
                            .toList();
                    return ResponseEntity.ok(sectionList);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
