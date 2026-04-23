package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByFacultyIdAndIsActiveTrue(Long facultyId);
    Optional<AttendanceSession> findByIdAndIsActiveTrue(Long id);
    Optional<AttendanceSession> findBySectionIdAndIsActiveTrue(Long sectionId);
}
