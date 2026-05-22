package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    public AnnouncementService(AnnouncementRepository repo, SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Announcements> getAll() {
        return repo.findAll();
    }

    public Announcements add(Announcements a){
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
        return repo.findRelevantAnnouncements(sectionId, collegeId);
    }

    public void delete(Long id){
        repo.deleteById(id);
    }

    public Announcements update(Long id, Announcements a) {
        a.setId(id);
        return repo.save(a);
    }
}
