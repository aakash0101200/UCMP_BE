package com.ucmp.ucmp_backend.validator;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminScopeValidator {

    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;

    /**
     * Retrieves the department scope for the logged-in admin.
     * Returns "ALL" for super admins, and the specific department name for scoped
     * admins.
     */
    public String getAdminDepartment(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));

        if ("ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment())) {
            return "ALL";
        }
        return adminUser.getDepartment();
    }

    /**
     * Enforces that the logged-in admin has access to the target department and
     * year.
     * Throws RuntimeException if not authorized.
     */
    public void enforceAccess(Authentication authentication, String targetDept, Integer targetYear) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));

        boolean isSuper = "ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment());
        if (isSuper) {
            return;
        }

        // 1. Department match
        if (adminUser.getDepartment() != null && !adminUser.getDepartment().equalsIgnoreCase(targetDept)) {
            throw new RuntimeException("Access Denied: You do not have access to department: " + targetDept);
        }

        // 2. Year scope match (if set)
        if (adminUser.getYearScope() != null) {
            if (targetYear == null || !adminUser.getYearScope().equals(targetYear)) {
                throw new RuntimeException("Access Denied: You do not have access to year: " + targetYear);
            }
        }
    }

    /**
     * Enforces access to a section.
     */
    public void enforceAccessToSection(Authentication authentication, Long sectionId) {
        if (sectionId == null)
            return;

        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));

        boolean isSuper = "ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null
                || "Administration".equalsIgnoreCase(adminUser.getDepartment());
        if (isSuper) {
            return;
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found: " + sectionId));

        String dept = section.getBatch() != null ? section.getBatch().getBatchName() : null;
        Integer year = section.getYear();

        enforceAccess(authentication, dept, year);
    }

    /**
     * Enforces access to a student.
     */
    public void enforceAccessToStudent(Authentication authentication, Student student) {
        if (student == null)
            return;

        String dept = student.getBatch() != null ? student.getBatch().getBatchName() : null;
        Integer year = null;
        try {
            if (student.getYear() != null) {
                year = Integer.parseInt(student.getYear());
            }
        } catch (NumberFormatException e) {
            // Ignore if student year is non-integer string
        }

        enforceAccess(authentication, dept, year);
    }
}
