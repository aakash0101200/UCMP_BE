package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcements, Long> {
    Optional<Announcements> findById(Long id);

}
