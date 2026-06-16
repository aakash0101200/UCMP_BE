package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class AnnouncementService {

    private final AnnouncementRepository repo;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;

    public AnnouncementService(
            AnnouncementRepository repo,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            SectionRepository sectionRepository) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lazyPurgeStaleAnnouncements() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            repo.deleteByCreatedAtBefore(cutoff);
        } catch (Exception e) {
            System.err.println("Failed to lazy purge stale announcements: " + e.getMessage());
        }
    }

    public List<Announcements> getAll() {
        lazyPurgeStaleAnnouncements();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return repo.findAllByOrderByAnnouncementIdDesc().stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                .toList();
    }

    public List<Announcements> getAllForUser(Authentication authentication) {
        lazyPurgeStaleAnnouncements();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        if (authentication == null) {
            return repo.findAllByOrderByAnnouncementIdDesc().stream()
                    .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                    .toList();
        }

        String collegeId = authentication.getName();
        User user = userRepository.findByCollegeId(collegeId).orElse(null);
        if (user == null) {
            return repo.findAllByOrderByAnnouncementIdDesc().stream()
                    .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                    .toList();
        }

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        boolean isFaculty = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("FACULTY"));
        boolean isStudent = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("STUDENT"));

        List<Announcements> all = repo.findAllByOrderByAnnouncementIdDesc().stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                .toList();

        if (isAdmin) {
            boolean isSuper = "ADMIN_001".equals(collegeId) || user.getDepartment() == null
                    || "Administration".equalsIgnoreCase(user.getDepartment());

            List<Announcements> adminFiltered = all.stream()
                    .filter(a -> {
                        // ── OPERATIONAL_LOG scope: admin gets monthly reports later, not live logs ──
                        if ("ATTENDANCE_WARNING".equalsIgnoreCase(a.getType()) ||
                                "ATTENDANCE_SESSION".equalsIgnoreCase(a.getType())) {
                            return false;
                        }
                        // ── PRIVATE_THREAD scope: faculty↔student messages are never admin-visible ──
                        if ("MESSAGE".equalsIgnoreCase(a.getType()) ||
                                "PRIORITY_MESSAGE".equalsIgnoreCase(a.getType()) ||
                                "REPLY".equalsIgnoreCase(a.getType())) {
                            return false;
                        }
                        // ── SYSTEM_BROADCAST scope: announcements, timetable, schedule visible ──
                        return true;
                    })
                    .toList();

            if (isSuper) {
                return adminFiltered;
            }

            final String dept = user.getDepartment();
            final Integer yearScope = user.getYearScope();

            return adminFiltered.stream()
                    .filter(a -> {
                        if (collegeId.equals(a.getAuthor()))
                            return true;
                        if (a.getTargetDept() != null && !a.getTargetDept().equalsIgnoreCase(dept))
                            return false;
                        if (a.getTargetYear() != null && !a.getTargetYear().equals(yearScope))
                            return false;
                        if (a.getSectionId() != null) {
                            Section sec = sectionRepository.findById(a.getSectionId()).orElse(null);
                            if (sec == null)
                                return false;
                            String secDept = sec.getBatch() != null ? sec.getBatch().getBatchName() : null;
                            Integer secYear = sec.getYear();
                            if (dept != null && !dept.equalsIgnoreCase(secDept))
                                return false;
                            if (yearScope != null && !yearScope.equals(secYear))
                                return false;
                        }
                        return true;
                    })
                    .toList();
        } else if (isFaculty) {
            Faculty faculty = facultyRepository.findByCollegeId(collegeId).orElse(null);
            final String dept = faculty != null ? faculty.getDepartment() : user.getDepartment();

            return all.stream()
                    .filter(a -> {
                        // Suppress low-level user operation notifications for faculty
                        if ("ATTENDANCE_WARNING".equalsIgnoreCase(a.getType()) ||
                                "ATTENDANCE_SESSION".equalsIgnoreCase(a.getType())) {
                            return false;
                        }

                        // 1. Messages sent by this faculty (where targetRole = facultyCollegeId)
                        if (("MESSAGE".equalsIgnoreCase(a.getType())
                                || "PRIORITY_MESSAGE".equalsIgnoreCase(a.getType()))
                                && collegeId.equalsIgnoreCase(a.getTargetRole())) {
                            return true;
                        }

                        // 2. Replies addressed to this faculty (where studentCollegeId =
                        // facultyCollegeId)
                        if ("REPLY".equalsIgnoreCase(a.getType())
                                && collegeId.equalsIgnoreCase(a.getStudentCollegeId())) {
                            return true;
                        }

                        // For other message/reply types that were not sent by or addressed to this
                        // faculty, hide them
                        if ("MESSAGE".equalsIgnoreCase(a.getType()) || "PRIORITY_MESSAGE".equalsIgnoreCase(a.getType())
                                || "REPLY".equalsIgnoreCase(a.getType())) {
                            return false;
                        }

                        if (a.getStudentCollegeId() != null)
                            return false;
                        if (a.getTargetRole() != null && !a.getTargetRole().equalsIgnoreCase("FACULTY"))
                            return false;
                        if (a.getTargetDept() != null && dept != null && !a.getTargetDept().equalsIgnoreCase(dept))
                            return false;
                        return true;
                    })
                    .toList();
        } else if (isStudent) {
            Student student = studentRepository.findByCollegeId(collegeId).orElse(null);
            final Long sectionId = (student != null && student.getSection() != null) ? student.getSection().getId()
                    : null;
            final Integer year = (student != null && student.getYear() != null) ? safeParseYear(student.getYear())
                    : null;
            final String dept = (student != null && student.getBatch() != null) ? student.getBatch().getBatchName()
                    : null;

            return all.stream()
                    .filter(a -> {
                        // 1. Replies sent by this student (where type = REPLY && targetRole =
                        // studentCollegeId)
                        if ("REPLY".equalsIgnoreCase(a.getType()) && collegeId.equalsIgnoreCase(a.getTargetRole())) {
                            return true;
                        }

                        // 2. Messages directly addressed to this student (studentCollegeId = student's
                        // collegeId)
                        if (collegeId.equalsIgnoreCase(a.getStudentCollegeId())) {
                            return true;
                        }

                        // 3. Section broadcasts (excluding REPLY types from other students in the
                        // section)
                        if (sectionId != null && sectionId.equals(a.getSectionId())) {
                            if ("REPLY".equalsIgnoreCase(a.getType())) {
                                return false; // Do not show other students' replies
                            }
                            return true;
                        }

                        // 4. Global announcements (no section, no student target)
                        if (a.getSectionId() == null && a.getStudentCollegeId() == null) {
                            if (a.getTargetRole() != null && !a.getTargetRole().equalsIgnoreCase("STUDENT"))
                                return false;
                            if (a.getTargetYear() != null && !a.getTargetYear().equals(year))
                                return false;
                            if (a.getTargetDept() != null && dept != null && !a.getTargetDept().equalsIgnoreCase(dept))
                                return false;
                            return true;
                        }
                        return false;
                    })
                    .toList();
        }

        return all;
    }

    public Announcements add(Announcements a) {
        Announcements saved = repo.save(a);
        if (saved.getStudentCollegeId() != null) {
            messagingTemplate.convertAndSend("/topic/notifications/student/" + saved.getStudentCollegeId(), saved);
        } else if (saved.getSectionId() == null) {
            messagingTemplate.convertAndSend("/topic/notifications/global", saved);
        } else {
            messagingTemplate.convertAndSend("/topic/notifications/section/" + saved.getSectionId(), saved);
        }
        return saved;
    }

    public List<Announcements> getAnnouncementsForStudent(Long sectionId) {
        lazyPurgeStaleAnnouncements();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return repo.findBySectionIdIsNullOrSectionIdOrderByAnnouncementIdDesc(sectionId).stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                .toList();
    }

    public List<Announcements> getAnnouncementsForStudent(Long sectionId, String collegeId) {
        lazyPurgeStaleAnnouncements();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        Student student = studentRepository.findByCollegeId(collegeId).orElse(null);
        final Integer year = (student != null && student.getYear() != null) ? safeParseYear(student.getYear())
                : null;
        final String dept = (student != null && student.getBatch() != null) ? student.getBatch().getBatchName() : null;

        return repo.findRelevantAnnouncements(sectionId, collegeId).stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                .filter(a -> {
                    if (a.getSectionId() == null && a.getStudentCollegeId() == null) {
                        if (a.getTargetRole() != null && !a.getTargetRole().equalsIgnoreCase("STUDENT"))
                            return false;
                        if (a.getTargetYear() != null && !a.getTargetYear().equals(year))
                            return false;
                        if (a.getTargetDept() != null && dept != null && !a.getTargetDept().equalsIgnoreCase(dept))
                            return false;
                    }
                    return true;
                })
                .toList();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Announcements update(Long id, Announcements a) {
        a.setId(id);
        return repo.save(a);
    }

    private Integer safeParseYear(String yearStr) {
        if (yearStr == null)
            return null;
        try {
            return Integer.parseInt(yearStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
