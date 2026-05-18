package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByStudentIdAndAttendanceSessionId(Long studentId, Long sessionId);

    List<AttendanceRecord> findByAttendanceSessionId(Long sessionId);

    List<AttendanceRecord> findByStudentIdOrderByMarkedAtDesc(Long studentId);

    // ── Section-wide attended count (for overall average) ─────────────────────
    @Query("SELECT COUNT(r) FROM AttendanceRecord r " +
           "WHERE r.student.id = :studentId " +
           "AND r.attendanceSession.section.id = :sectionId")
    long countByStudentIdAndSectionId(
            @Param("studentId") Long studentId,
            @Param("sectionId") Long sectionId);

    // ── Per-subject: attended sessions tagged with a specific subject ──────────
    // Correct even when Prof. Sharma teaches two subjects to the same section.
    @Query("SELECT COUNT(r) FROM AttendanceRecord r " +
           "WHERE r.student.id = :studentId " +
           "AND r.attendanceSession.subject.id = :subjectId " +
           "AND r.attendanceSession.section.id = :sectionId")
    long countByStudentIdAndSubjectIdAndSectionId(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("sectionId") Long sectionId);

    // ── Fallback: attended count for old untagged sessions (by faculty) ────────
    @Query("SELECT COUNT(r) FROM AttendanceRecord r " +
           "WHERE r.student.id = :studentId " +
           "AND r.attendanceSession.faculty.id = :facultyId " +
           "AND r.attendanceSession.section.id = :sectionId " +
           "AND r.attendanceSession.subject IS NULL")
    long countUntaggedByStudentIdAndFacultyIdAndSectionId(
            @Param("studentId") Long studentId,
            @Param("facultyId") Long facultyId,
            @Param("sectionId") Long sectionId);
}
