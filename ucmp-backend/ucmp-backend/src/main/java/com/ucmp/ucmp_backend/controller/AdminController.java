package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AdminCreateFacultyRequest;
import com.ucmp.ucmp_backend.dto.AdminCreateStudentRequest;
import com.ucmp.ucmp_backend.dto.AdminUserDTO;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import com.ucmp.ucmp_backend.service.AuthService;
import com.ucmp.ucmp_backend.service.UserDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.HashSet;
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
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDeletionService userDeletionService;

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
                if (user.getFaculty().getSections() != null) {
                    List<Long> sIds = user.getFaculty().getSections().stream()
                            .map(Section::getId)
                            .collect(Collectors.toList());
                    builder.sectionIds(sIds);
                }
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

    @PutMapping("/users/{collegeId}/reset-password")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> resetPassword(@PathVariable String collegeId) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String defaultPassword = "User@123";
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN)) {
            defaultPassword = "Admin@123";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.FACULTY)) {
            defaultPassword = "Faculty@123";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.STUDENT)) {
            defaultPassword = "Student@123";
        }

        user.setPassword(passwordEncoder.encode(defaultPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Password reset successfully. Default password is: " + defaultPassword);
    }

    @PutMapping("/users/{collegeId}/student-section")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateStudentSection(@PathVariable String collegeId, @RequestBody Map<String, Long> payload) {
        Long sectionId = payload.get("sectionId");
        if (sectionId == null) {
            throw new RuntimeException("Section ID is required");
        }
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Student student = user.getStudent();
        if (student == null) {
            throw new RuntimeException("User is not a student");
        }
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        student.setSection(section);
        if (section.getBatch() != null) {
            student.setBatch(section.getBatch());
        }
        studentRepository.save(student);
        return ResponseEntity.ok("Student section updated successfully.");
    }

    @PutMapping("/users/{collegeId}/faculty-sections")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateFacultySections(@PathVariable String collegeId, @RequestBody Map<String, List<Long>> payload) {
        List<Long> sectionIds = payload.get("sectionIds");
        if (sectionIds == null) {
            throw new RuntimeException("Section IDs are required");
        }
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Faculty faculty = user.getFaculty();
        if (faculty == null) {
            throw new RuntimeException("User is not a faculty member");
        }

        List<Section> sections = sectionRepository.findAllById(sectionIds);
        faculty.setSections(new HashSet<>(sections));
        facultyRepository.save(faculty);
        return ResponseEntity.ok("Faculty sections updated successfully.");
    }

    @DeleteMapping("/users/{collegeId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String collegeId) {
        userDeletionService.deleteUserCascaded(collegeId);
        return ResponseEntity.ok("User deleted successfully.");
    }
}