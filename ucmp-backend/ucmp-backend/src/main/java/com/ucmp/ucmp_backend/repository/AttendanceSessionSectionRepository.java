package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.AttendanceSessionSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceSessionSectionRepository extends JpaRepository<AttendanceSessionSection, Long> {

    // All section-rows for a given session
    List<AttendanceSessionSection> findBySessionId(Long sessionId);

    // All section IDs participating in a session (for quick membership check)
    @Query("SELECT ass.section.id FROM AttendanceSessionSection ass WHERE ass.session.id = :sessionId")
    List<Long> findSectionIdsBySessionId(@Param("sessionId") Long sessionId);

    // Find if an active session exists for a given section (including merged sessions)
    // Used in findActiveSessionForStudent
    @Query("""
        SELECT ass FROM AttendanceSessionSection ass
        WHERE ass.section.id = :sectionId
        AND ass.session.isActive = true
    """)
    List<AttendanceSessionSection> findActiveSectionMemberships(@Param("sectionId") Long sectionId);

    // All sessions (active or historical) that included a specific section
    @Query("SELECT ass FROM AttendanceSessionSection ass WHERE ass.section.id = :sectionId")
    List<AttendanceSessionSection> findBySectionId(@Param("sectionId") Long sectionId);
}
