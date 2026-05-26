package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Subject;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.SubjectRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private String enforceAdminRoleAndDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));
        
        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
            return "ALL";
        }
        return adminUser.getDepartment();
    }

    /** GET /api/subjects — All subjects */
    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(subjectRepository.findAll());
    }

    /** GET /api/subjects/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubject(@PathVariable Long id) {
        return subjectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/subjects/department/{dept} — e.g. /api/subjects/department/Computer Science */
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Subject>> getByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(subjectRepository.findByDepartment(department));
    }

    /** POST /api/subjects — Create subject (Admin only) */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSubject(Authentication authentication, @RequestBody Subject subject) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept) && !dept.equalsIgnoreCase(subject.getDepartment())) {
            throw new RuntimeException("Access Denied: You can only create subjects inside your own department: " + dept);
        }
        if (subjectRepository.existsByCode(subject.getCode())) {
            return ResponseEntity.badRequest()
                    .body("Subject with code '" + subject.getCode() + "' already exists");
        }
        return ResponseEntity.ok(subjectRepository.save(subject));
    }

    /** PUT /api/subjects/{id} — Update subject (Admin only) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateSubject(Authentication authentication, @PathVariable Long id, @RequestBody Subject updated) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            if (!dept.equalsIgnoreCase(updated.getDepartment())) {
                throw new RuntimeException("Access Denied: You can only update subjects inside your own department: " + dept);
            }
            Subject subject = subjectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subject not found"));
            if (!dept.equalsIgnoreCase(subject.getDepartment())) {
                throw new RuntimeException("Access Denied: You can only modify subjects inside your own department: " + dept);
            }
        }
        return subjectRepository.findById(id).map(subject -> {
            subject.setName(updated.getName());
            subject.setCode(updated.getCode());
            subject.setCredits(updated.getCredits());
            subject.setWeeklyHours(updated.getWeeklyHours());
            subject.setSlotDuration(updated.getSlotDuration());
            subject.setRequiredRoomType(updated.getRequiredRoomType());
            subject.setDepartment(updated.getDepartment());
            return ResponseEntity.ok(subjectRepository.save(subject));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/subjects/{id} (Admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteSubject(Authentication authentication, @PathVariable Long id) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            Subject subject = subjectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subject not found"));
            if (!dept.equalsIgnoreCase(subject.getDepartment())) {
                throw new RuntimeException("Access Denied: You can only delete subjects inside your own department: " + dept);
            }
        }
        if (!subjectRepository.existsById(id)) return ResponseEntity.notFound().build();
        subjectRepository.deleteById(id);
        return ResponseEntity.ok("Subject deleted");
    }
}
