package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AdminCreateFacultyRequest;
import com.ucmp.ucmp_backend.dto.AdminCreateStudentRequest;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.service.AuthService;
import com.ucmp.ucmp_backend.dto.AdminUserDTO;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.model.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

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

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getStats() {
        long studentCount = studentRepository.count();
        long facultyCount = facultyRepository.count();
        long totalUsers = userRepository.count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "studentCount", studentCount,
                "facultyCount", facultyCount
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserDTO> dtos = users.stream().map(user -> {
            String roleName = "STUDENT";
            if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN)) {
                roleName = "ADMIN";
            } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.FACULTY)) {
                roleName = "FACULTY";
            }

            AdminUserDTO.AdminUserDTOBuilder builder = AdminUserDTO.builder()
                    .collegeId(user.getCollegeId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(roleName);

            if ("ADMIN".equals(roleName)) {
                builder.department("Administration");
            } else if ("FACULTY".equals(roleName) && user.getFaculty() != null) {
                builder.department(user.getFaculty().getDepartment());
            } else if ("STUDENT".equals(roleName) && user.getStudent() != null) {
                var student = user.getStudent();
                builder.rollNumber(student.getRollNumber());
                builder.year(student.getYear());
                if (student.getBatch() != null) {
                    builder.branch(student.getBatch().getBatchName());
                }
                if (student.getSection() != null) {
                    builder.sectionName(student.getSection().getSectionName());
                }
            }
            return builder.build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}