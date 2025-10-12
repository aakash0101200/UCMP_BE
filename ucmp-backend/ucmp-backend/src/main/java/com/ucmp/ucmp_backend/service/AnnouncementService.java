package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private final AnnouncementRepository repo;

    public AnnouncementService(AnnouncementRepository repo) {
        this.repo = repo;
    }

    public List<Announcements> getAll() {
        return repo.findAll();
    }

    public Announcements add(Announcements a){
        return repo.save(a);
    }

    public void delete(Long id){
        repo.deleteById(id);
    }

    public Announcements update(Long id, Announcements a) {
        a.setId(id);
        return repo.save(a);
    }
}
