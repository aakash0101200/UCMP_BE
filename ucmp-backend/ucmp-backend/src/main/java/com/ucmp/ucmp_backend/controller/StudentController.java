package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
import com.ucmp.ucmp_backend.service.StudentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    // GET: fetch profile by collegeId (useful for profile cards)
    @GetMapping("/{collegeId}/profile")
    public ResponseEntity<StudentProfileDTO> getProfile(@PathVariable String collegeId) {
        return studentService.getStudentProfileByCollegeId(collegeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST: create new student+profile
    @PostMapping
    public ResponseEntity<StudentProfileDTO> createProfile(@RequestBody StudentProfileDTO dto) {
        StudentProfileDTO created = studentService.createStudentProfile(dto);
        return ResponseEntity.ok(created);
    }

    // PUT: update existing student+profile
    @PutMapping("/{collegeId}/profile")
    public ResponseEntity<StudentProfileDTO> updateProfile(
            @PathVariable String collegeId,
            @RequestBody StudentProfileDTO dto) {
        return studentService.updateStudentProfile(collegeId, dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


}
