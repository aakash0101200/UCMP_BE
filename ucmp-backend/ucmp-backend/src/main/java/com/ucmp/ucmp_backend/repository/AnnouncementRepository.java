package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcements, Long> {
    Optional<Announcements> findById(Long userId);

    List<Announcements> findAllByOrderByAnnouncementIdDesc();

    List<Announcements> findBySectionIdIsNullOrSectionIdOrderByAnnouncementIdDesc(Long sectionId);

    @Query("SELECT a FROM Announcements a WHERE " +
            "(a.sectionId IS NULL AND a.studentCollegeId IS NULL) OR " +
            "(a.sectionId = :sectionId AND a.studentCollegeId IS NULL) OR " +
            "(a.studentCollegeId = :collegeId) " +
            "ORDER BY a.announcementId DESC")
    List<Announcements> findRelevantAnnouncements(@Param("sectionId") Long sectionId,
            @Param("collegeId") String collegeId);
}
