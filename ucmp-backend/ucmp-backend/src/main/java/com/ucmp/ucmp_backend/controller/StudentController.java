package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.service.StudentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    // GET: fetch profile by collegeId (useful for profile cards)
//    @GetMapping("/{collegeId}/profile")
//    public ResponseEntity<StudentProfileDTO> getProfile(@PathVariable String collegeId) {
//        return studentService.getStudentProfileByCollegeId(collegeId)
//                .map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    // POST: create new student+profile
//    @PostMapping
//    public ResponseEntity<StudentProfileDTO> createProfile(@RequestBody StudentProfileDTO dto) {
//        return ResponseEntity.ok(studentService.createStudentProfile(dto));
//    }
//
//    // PUT: update existing student+profile
//    @PutMapping("/{collegeId}/profile")
//    public ResponseEntity<StudentProfileDTO> updateProfile(
//            @PathVariable String collegeId,
//            @RequestBody StudentProfileDTO dto) {
//        return studentService.updateStudentProfile(collegeId, dto)
//                .map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }

    @PostMapping
    public Student createStudent(@RequestBody Student student) {
        return studentService.saveStudent(student);
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    @GetMapping("/{id}/faculties")
    public Set<Faculty> getFacultiesForStudent(@PathVariable Long id) {
        return studentService.getFacultiesForStudent(id);
    }

}
