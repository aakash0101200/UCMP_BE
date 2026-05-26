package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import com.ucmp.ucmp_backend.service.AnnouncementService;
import com.ucmp.ucmp_backend.validator.AdminScopeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService service;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final AnnouncementRepository announcementRepository;
    private final AdminScopeValidator adminScopeValidator;

    private String enforceAdminRoleAndDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));
        
        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
            return "ALL";
        }
        return adminUser.getDepartment();
    }

    @GetMapping("/")
    public List<Announcements> getAll(Authentication authentication) {
        return service.getAllForUser(authentication);
    }

    @GetMapping("/section/{sectionId}")
    public List<Announcements> getBySection(@PathVariable Long sectionId) {
        return service.getAnnouncementsForStudent(sectionId);
    }

    @GetMapping("/student/{collegeId}/section/{sectionId}")
    public List<Announcements> getForStudent(@PathVariable String collegeId, @PathVariable Long sectionId) {
        return service.getAnnouncementsForStudent(sectionId, collegeId);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> create(Authentication authentication, @RequestBody Announcements a) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            if (a.getSectionId() == null) {
                return ResponseEntity.status(403).body("Access Denied: Departmental/Year-scoped Admins cannot create global announcements.");
            }
            adminScopeValidator.enforceAccessToSection(authentication, a.getSectionId());
        }
        return ResponseEntity.ok(service.add(a));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            Announcements existing = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));
            if (existing.getSectionId() == null) {
                return ResponseEntity.status(403).body("Access Denied: Departmental/Year-scoped Admins cannot delete global announcements.");
            }
            adminScopeValidator.enforceAccessToSection(authentication, existing.getSectionId());
        }
        service.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @RequestBody Announcements a) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            Announcements existing = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));
            if (existing.getSectionId() == null || a.getSectionId() == null) {
                return ResponseEntity.status(403).body("Access Denied: Departmental/Year-scoped Admins cannot manage global announcements.");
            }
            adminScopeValidator.enforceAccessToSection(authentication, existing.getSectionId());
            adminScopeValidator.enforceAccessToSection(authentication, a.getSectionId());
        }
        a.setId(id);
        return ResponseEntity.ok(service.update(id, a));
    }
}
