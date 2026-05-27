package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;

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

    public List<Announcements> getAll() {
        return repo.findAllByOrderByAnnouncementIdDesc();
    }

    public List<Announcements> getAllForUser(Authentication authentication) {
        if (authentication == null) {
            return repo.findAllByOrderByAnnouncementIdDesc();
        }

        String collegeId = authentication.getName();
        User user = userRepository.findByCollegeId(collegeId).orElse(null);
        if (user == null) {
            return repo.findAllByOrderByAnnouncementIdDesc();
        }

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        boolean isFaculty = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("FACULTY"));
        boolean isStudent = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("STUDENT"));

        List<Announcements> all = repo.findAllByOrderByAnnouncementIdDesc();

        if (isAdmin) {
            boolean isSuper = "ADMIN_001".equals(collegeId) || user.getDepartment() == null
                    || "Administration".equalsIgnoreCase(user.getDepartment());
            if (isSuper) {
                return all;
            }

            final String dept = user.getDepartment();
            final Integer yearScope = user.getYearScope();

            return all.stream()
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
                        if (collegeId.equals(a.getAuthor()))
                            return true;
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
            final Integer year = (student != null && student.getYear() != null) ? Integer.parseInt(student.getYear())
                    : null;
            final String dept = (student != null && student.getBatch() != null) ? student.getBatch().getBatchName()
                    : null;

            return all.stream()
                    .filter(a -> {
                        if (collegeId.equalsIgnoreCase(a.getStudentCollegeId()))
                            return true;
                        if (sectionId != null && sectionId.equals(a.getSectionId()))
                            return true;
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
        return repo.findBySectionIdIsNullOrSectionIdOrderByAnnouncementIdDesc(sectionId);
    }

    public List<Announcements> getAnnouncementsForStudent(Long sectionId, String collegeId) {
        Student student = studentRepository.findByCollegeId(collegeId).orElse(null);
        final Integer year = (student != null && student.getYear() != null) ? Integer.parseInt(student.getYear())
                : null;
        final String dept = (student != null && student.getBatch() != null) ? student.getBatch().getBatchName() : null;

        return repo.findRelevantAnnouncements(sectionId, collegeId).stream()
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
}
