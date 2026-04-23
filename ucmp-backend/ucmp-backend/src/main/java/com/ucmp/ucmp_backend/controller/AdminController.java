package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AdminCreateFacultyRequest;
import com.ucmp.ucmp_backend.dto.AdminCreateStudentRequest;
import com.ucmp.ucmp_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PostMapping("/faculty")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createFaculty(@RequestBody AdminCreateFacultyRequest request) {
        authService.adminCreateFaculty(request);
        return ResponseEntity.ok("Faculty created and sections assigned successfully.");
    }
    @PostMapping("/student")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createStudent(@RequestBody AdminCreateStudentRequest request) {
        authService.adminCreateStudent(request);
        return ResponseEntity.ok("Student created and assigned to batch/section successfully.");
    }
}