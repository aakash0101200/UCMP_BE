package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.AdminCreateFacultyRequest;
import com.ucmp.ucmp_backend.dto.AdminCreateStudentRequest;
import com.ucmp.ucmp_backend.dto.AdminUserDTO;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import com.ucmp.ucmp_backend.service.AuthService;
import com.ucmp.ucmp_backend.service.UserDeletionService;
import com.ucmp.ucmp_backend.validator.AdminScopeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDeletionService userDeletionService;
    private final AdminScopeValidator adminScopeValidator;

    private String enforceAdminRoleAndDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not an Admin"));

        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
            return "ALL";
        }
        return adminUser.getDepartment();
    }

    @PostMapping("/faculty")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createFaculty(Authentication authentication,
            @RequestBody AdminCreateFacultyRequest request) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept) && !dept.equalsIgnoreCase(request.getDepartment())) {
            throw new RuntimeException(
                    "Access Denied: You can only create Faculty members inside your own department: " + dept);
        }
        authService.adminCreateFaculty(request);
        return ResponseEntity.ok("Faculty created and sections assigned successfully.");
    }

    @PostMapping("/student")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createStudent(Authentication authentication,
            @RequestBody AdminCreateStudentRequest request) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            Batch batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Batch not found"));
            if (!dept.equalsIgnoreCase(batch.getBatchName())) {
                throw new RuntimeException(
                        "Access Denied: You can only create Students inside your own department: " + dept);
            }
            adminScopeValidator.enforceAccess(authentication, batch.getBatchName(), request.getYear());
        }
        authService.adminCreateStudent(request);
        return ResponseEntity.ok("Student created and assigned to batch/section successfully.");
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getStats(Authentication authentication) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        long studentCount, facultyCount, totalUsers;
        if ("ALL".equals(dept)) {
            studentCount = studentRepository.count();
            facultyCount = facultyRepository.count();
            totalUsers = userRepository.count();
        } else {
            studentCount = studentRepository.findAll().stream()
                    .filter(s -> s.getBatch() != null && dept.equalsIgnoreCase(s.getBatch().getBatchName()))
                    .count();
            facultyCount = facultyRepository.findAll().stream()
                    .filter(f -> dept.equalsIgnoreCase(f.getDepartment()))
                    .count();
            totalUsers = studentCount + facultyCount;
        }
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "studentCount", studentCount,
                "facultyCount", facultyCount));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(Authentication authentication) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        User adminUser = userRepository.findByCollegeId(authentication.getName()).orElseThrow();
        List<User> users;
        if ("ALL".equals(dept)) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findAll().stream()
                    .filter(u -> {
                        if (u.getFaculty() != null && dept.equalsIgnoreCase(u.getFaculty().getDepartment())) {
                            return true;
                        }
                        if (u.getStudent() != null && u.getStudent().getBatch() != null
                                && dept.equalsIgnoreCase(u.getStudent().getBatch().getBatchName())) {
                            if (adminUser.getYearScope() != null) {
                                return String.valueOf(adminUser.getYearScope()).equals(u.getStudent().getYear());
                            }
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

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
                    .role(roleName)
                    .department(user.getDepartment())
                    .yearScope(user.getYearScope());

            if ("ADMIN".equals(roleName)) {
                if (user.getDepartment() == null) {
                    builder.department("Administration");
                }
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
    public ResponseEntity<?> resetPassword(Authentication authentication, @PathVariable String collegeId) {
        String callerCollegeId = authentication.getName();
        String dept = enforceAdminRoleAndDepartment(authentication);
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean targetIsAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);

        if (targetIsAdmin) {
            // Only a Super Admin ("ALL" dept) can reset an Admin's password
            if (!"ALL".equals(dept)) {
                throw new RuntimeException("Access Denied: Only Super Admins can reset Administrator passwords.");
            }
            // Block resetting the primary Super Admin (ADMIN_001) unless they are doing it
            // themselves
            if ("ADMIN_001".equals(collegeId) && !"ADMIN_001".equals(callerCollegeId)) {
                throw new RuntimeException(
                        "Access Denied: The primary Super Admin password cannot be reset by other administrators.");
            }
        } else {
            // Check regular admin scope for student/faculty
            if (!"ALL".equals(dept)) {
                boolean sameDept = false;
                if (user.getFaculty() != null && dept.equalsIgnoreCase(user.getFaculty().getDepartment())) {
                    sameDept = true;
                }
                if (user.getStudent() != null && user.getStudent().getBatch() != null
                        && dept.equalsIgnoreCase(user.getStudent().getBatch().getBatchName())) {
                    User adminUser = userRepository.findByCollegeId(callerCollegeId).orElseThrow();
                    if (adminUser.getYearScope() == null
                            || String.valueOf(adminUser.getYearScope()).equals(user.getStudent().getYear())) {
                        sameDept = true;
                    }
                }
                if (!sameDept) {
                    throw new RuntimeException(
                            "Access Denied: You cannot modify users outside your department and year scope.");
                }
            }
        }

        // Generate lookalike-safe random temporary password
        String tempPassword = generateRandomAlphanumericPassword(8);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully.",
                "temporaryPassword", tempPassword));
    }

    private String generateRandomAlphanumericPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"; // Omitted I, O, 0, l, 1
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @PutMapping("/users/{collegeId}/student-section")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateStudentSection(Authentication authentication, @PathVariable String collegeId,
            @RequestBody Map<String, Long> payload) {
        String dept = enforceAdminRoleAndDepartment(authentication);
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

        if (!"ALL".equals(dept)) {
            if (student.getBatch() == null || !dept.equalsIgnoreCase(student.getBatch().getBatchName())) {
                throw new RuntimeException(
                        "Access Denied: You cannot modify students outside your department: " + dept);
            }
            if (section.getBatch() == null || !dept.equalsIgnoreCase(section.getBatch().getBatchName())) {
                throw new RuntimeException("Access Denied: Target section does not belong to your department: " + dept);
            }
            adminScopeValidator.enforceAccessToStudent(authentication, student);
            adminScopeValidator.enforceAccessToSection(authentication, sectionId);
        }

        student.setSection(section);
        if (section.getBatch() != null) {
            student.setBatch(section.getBatch());
            user.setDepartment(section.getBatch().getBatchName());
        }
        studentRepository.save(student);
        return ResponseEntity.ok("Student section updated successfully.");
    }

    @PutMapping("/users/{collegeId}/faculty-sections")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateFacultySections(Authentication authentication, @PathVariable String collegeId,
            @RequestBody Map<String, List<Long>> payload) {
        String dept = enforceAdminRoleAndDepartment(authentication);
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

        if (!"ALL".equals(dept)) {
            if (!dept.equalsIgnoreCase(faculty.getDepartment())) {
                throw new RuntimeException(
                        "Access Denied: You cannot modify faculty members outside your department: " + dept);
            }
        }

        List<Section> sections = sectionRepository.findAllById(sectionIds);
        faculty.setSections(new HashSet<>(sections));
        facultyRepository.save(faculty);
        return ResponseEntity.ok("Faculty sections updated successfully.");
    }

    @DeleteMapping("/users/{collegeId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteUser(Authentication authentication, @PathVariable String collegeId) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ALL".equals(dept)) {
            boolean sameDept = false;
            if (user.getFaculty() != null && dept.equalsIgnoreCase(user.getFaculty().getDepartment())) {
                sameDept = true;
            }
            if (user.getStudent() != null && user.getStudent().getBatch() != null
                    && dept.equalsIgnoreCase(user.getStudent().getBatch().getBatchName())) {
                sameDept = true;
            }
            if (!sameDept) {
                throw new RuntimeException("Access Denied: You cannot delete users outside your department: " + dept);
            }
        }

        userDeletionService.deleteUserCascaded(collegeId);
        return ResponseEntity.ok("User deleted successfully.");
    }

    @GetMapping("/users/departments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<String>> getDepartments(Authentication authentication) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            return ResponseEntity.ok(List.of(dept));
        }

        Set<String> depts = new HashSet<>();
        depts.add("Administration");

        // Add departments from faculty
        List<Faculty> faculties = facultyRepository.findAll();
        for (Faculty f : faculties) {
            if (f.getDepartment() != null && !f.getDepartment().trim().isEmpty()) {
                depts.add(f.getDepartment().trim());
            }
        }

        // Add branches from batches
        List<Batch> batches = batchRepository.findAll();
        for (Batch b : batches) {
            if (b.getBatchName() != null && !b.getBatchName().trim().isEmpty()) {
                depts.add(b.getBatchName().trim());
            }
        }

        return ResponseEntity.ok(depts.stream().sorted().collect(Collectors.toList()));
    }

    @GetMapping("/users/departments/{deptName}/sections")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentSections(@PathVariable String deptName) {
        if ("Administration".equalsIgnoreCase(deptName)) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }

        List<Section> sections = sectionRepository.findAll();
        List<Map<String, Object>> response = new java.util.ArrayList<>();
        for (Section sec : sections) {
            if (sec.getBatch() != null && deptName.equalsIgnoreCase(sec.getBatch().getBatchName())) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", sec.getId());
                map.put("sectionName", sec.getSectionName());
                map.put("batchName", sec.getBatch().getBatchName());
                response.add(map);
            }
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/departments/{deptName}/faculty")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentFaculty(
            @PathVariable String deptName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        Map<String, Object> response = new java.util.HashMap<>();

        if ("Administration".equalsIgnoreCase(deptName)) {
            List<User> admins = new java.util.ArrayList<>();
            for (User u : userRepository.findAll()) {
                if (u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN)) {
                    admins.add(u);
                }
            }

            int start = Math.min(page * size, admins.size());
            int end = Math.min((page + 1) * size, admins.size());
            List<User> paginatedAdmins = admins.subList(start, end);

            List<AdminUserDTO> content = new java.util.ArrayList<>();
            for (User user : paginatedAdmins) {
                AdminUserDTO dto = AdminUserDTO.builder()
                        .collegeId(user.getCollegeId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role("ADMIN")
                        .department(user.getDepartment() != null ? user.getDepartment() : "Administration")
                        .build();
                content.add(dto);
            }

            response.put("content", content);
            response.put("last", end >= admins.size());
            response.put("totalElements", (long) admins.size());
            return ResponseEntity.ok(response);
        }

        List<Faculty> faculties = new java.util.ArrayList<>();
        for (Faculty f : facultyRepository.findAll()) {
            if (deptName.equalsIgnoreCase(f.getDepartment())) {
                faculties.add(f);
            }
        }

        int start = Math.min(page * size, faculties.size());
        int end = Math.min((page + 1) * size, faculties.size());
        List<Faculty> paginatedFaculty = faculties.subList(start, end);

        List<AdminUserDTO> content = new java.util.ArrayList<>();
        for (Faculty f : paginatedFaculty) {
            User user = f.getUser();
            List<Long> sIds = new java.util.ArrayList<>();
            if (f.getSections() != null) {
                for (Section s : f.getSections()) {
                    sIds.add(s.getId());
                }
            }

            AdminUserDTO dto = AdminUserDTO.builder()
                    .collegeId(f.getCollegeId())
                    .name(user != null ? user.getName() : f.getCollegeId())
                    .email(user != null ? user.getEmail() : "")
                    .role("FACULTY")
                    .department(f.getDepartment())
                    .designation(f.getDesignation())
                    .sectionIds(sIds)
                    .build();
            content.add(dto);
        }

        response.put("content", content);
        response.put("last", end >= faculties.size());
        response.put("totalElements", (long) faculties.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/sections/{sectionId}/students")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSectionStudents(
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        List<Student> students = studentRepository.findBySectionId(sectionId);

        int start = Math.min(page * size, students.size());
        int end = Math.min((page + 1) * size, students.size());
        List<Student> paginatedStudents = students.subList(start, end);

        List<AdminUserDTO> content = new java.util.ArrayList<>();
        for (Student s : paginatedStudents) {
            User user = s.getUser();
            AdminUserDTO dto = AdminUserDTO.builder()
                    .collegeId(s.getCollegeId())
                    .name(s.getName())
                    .email(user != null ? user.getEmail() : "")
                    .role("STUDENT")
                    .rollNumber(s.getRollNumber())
                    .year(s.getYear())
                    .branch(s.getBatch() != null ? s.getBatch().getBatchName() : "")
                    .sectionName(s.getSection() != null ? s.getSection().getSectionName() : "")
                    .build();
            content.add(dto);
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", content);
        response.put("last", end >= students.size());
        response.put("totalElements", (long) students.size());
        return ResponseEntity.ok(response);
    }
}