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
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService service;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final AnnouncementRepository announcementRepository;
    private final AdminScopeValidator adminScopeValidator;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;

    private String enforceAdminRoleAndDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));

        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
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
                return ResponseEntity.status(403)
                        .body("Access Denied: Departmental/Year-scoped Admins cannot create global announcements.");
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
                return ResponseEntity.status(403)
                        .body("Access Denied: Departmental/Year-scoped Admins cannot delete global announcements.");
            }
            adminScopeValidator.enforceAccessToSection(authentication, existing.getSectionId());
        }
        service.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id,
            @RequestBody Announcements a) {
        String dept = enforceAdminRoleAndDepartment(authentication);
        if (!"ALL".equals(dept)) {
            Announcements existing = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));
            if (existing.getSectionId() == null || a.getSectionId() == null) {
                return ResponseEntity.status(403)
                        .body("Access Denied: Departmental/Year-scoped Admins cannot manage global announcements.");
            }
            adminScopeValidator.enforceAccessToSection(authentication, existing.getSectionId());
            adminScopeValidator.enforceAccessToSection(authentication, a.getSectionId());
        }
        a.setId(id);
        return ResponseEntity.ok(service.update(id, a));
    }

    // ─── Quick-Connect: Faculty sends a message to section or individual student ──
    @PostMapping("/message")
    @PreAuthorize("hasAuthority('FACULTY')")
    public ResponseEntity<?> sendMessage(Authentication authentication, @RequestBody Announcements msg) {
        String collegeId = authentication.getName();
        Faculty faculty = facultyRepository.findByCollegeId(collegeId).orElse(null);
        if (faculty == null) {
            return ResponseEntity.status(403).body("Faculty profile not found.");
        }

        // Validate faculty teaches in the target section
        if (msg.getSectionId() != null) {
            boolean teachesSection = faculty.getSections().stream()
                    .anyMatch(s -> s.getId().equals(msg.getSectionId()));
            if (!teachesSection) {
                return ResponseEntity.status(403)
                        .body("You do not teach in the specified section.");
            }
        }

        // Enforce message metadata
        String userName = faculty.getUser() != null ? faculty.getUser().getName() : collegeId;
        msg.setAuthor(userName);
        msg.setTime(Instant.now().toString());
        if (msg.getType() == null || msg.getType().isBlank()) {
            msg.setType("MESSAGE");
        }
        msg.setCompleted(false);
        msg.setTargetRole(collegeId); // Store faculty collegeId in targetRole

        return ResponseEntity.ok(service.add(msg));
    }

    // ─── Quick-Connect: Student acknowledges a message ─────────────────────────
    @PatchMapping("/{id}/ack")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    public ResponseEntity<?> acknowledge(@PathVariable Long id, Authentication authentication) {
        Announcements ann = announcementRepository.findById(id).orElse(null);
        if (ann == null) {
            return ResponseEntity.notFound().build();
        }
        ann.setCompleted(true);
        announcementRepository.save(ann);
        return ResponseEntity.ok(Map.of("status", "acknowledged", "id", id));
    }

    // ─── Quick-Connect: Student replies to a faculty message ───────────────────
    @PatchMapping("/{id}/reply")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    public ResponseEntity<?> replyToMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String collegeId = authentication.getName();
        String replyText = body.getOrDefault("reply", "").trim();
        if (replyText.isEmpty() || replyText.length() > 500) {
            return ResponseEntity.badRequest().body("Reply must be between 1 and 500 characters.");
        }

        Announcements parent = announcementRepository.findById(id).orElse(null);
        if (parent == null) {
            return ResponseEntity.notFound().build();
        }

        // Create a reply as a new announcement linked to the parent's context
        User user = userRepository.findByCollegeId(collegeId).orElse(null);
        String authorName = user != null ? user.getName() : collegeId;

        // Determine recipient (faculty college ID) from parent targetRole, fallback to username lookup
        String facultyCollegeId = parent.getTargetRole();
        if (facultyCollegeId == null || facultyCollegeId.trim().isEmpty()) {
            facultyCollegeId = userRepository.findAll().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(parent.getAuthor()))
                    .findFirst()
                    .map(User::getCollegeId)
                    .orElse(parent.getAuthor());
        }

        Announcements reply = new Announcements();
        reply.setTitle("Re: " + (parent.getTitle() != null ? parent.getTitle() : "Message"));
        reply.setDescription(replyText);
        reply.setAuthor(authorName);
        reply.setTime(Instant.now().toString());
        reply.setType("REPLY");
        reply.setSectionId(parent.getSectionId());
        reply.setStudentCollegeId(facultyCollegeId); // Route reply back to faculty collegeId
        reply.setTargetRole(collegeId); // Store student collegeId in targetRole
        reply.setLocation(String.valueOf(parent.getId())); // Link reply to parent message by storing parent ID in location
        reply.setCompleted(false);

        Announcements saved = announcementRepository.save(reply);
        return ResponseEntity.ok(saved);
    }
}
