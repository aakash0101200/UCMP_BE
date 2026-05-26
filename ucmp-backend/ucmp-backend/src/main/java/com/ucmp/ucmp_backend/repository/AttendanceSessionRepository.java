package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.AttendanceSession;
import com.ucmp.ucmp_backend.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    List<AttendanceSession> findByFacultyId(Long facultyId);

    @Modifying
    @Query("UPDATE AttendanceSession s SET s.scheduledFaculty = null WHERE s.scheduledFaculty.id = :facultyId")
    void nullifyScheduledFacultyReferences(@Param("facultyId") Long facultyId);

    // ── Legacy isActive-based finders (kept for backward compat) ──────────────
    List<AttendanceSession> findByFacultyIdAndIsActiveTrue(Long facultyId);

    Optional<AttendanceSession> findByIdAndIsActiveTrue(Long id);

    // ── Status-based finders (preferred going forward) ─────────────────────────
    List<AttendanceSession> findByFacultyIdAndStatus(Long facultyId, SessionStatus status);

    // ── Find active session for a section (REGULAR sessions) ──────────────────
    Optional<AttendanceSession> findBySectionIdAndIsActiveTrue(Long sectionId);

    // ── Find active session for a section INCLUDING merged sessions ────────────
    // Checks both: the primary section field AND the join table.
    // This is the correct lookup used by findActiveSessionForStudent.
    @Query("""
                SELECT s FROM AttendanceSession s
                WHERE s.isActive = true
                AND (
                    s.section.id = :sectionId
                    OR EXISTS (
                        SELECT 1 FROM AttendanceSessionSection ass
                        WHERE ass.session = s AND ass.section.id = :sectionId
                    )
                )
            """)
    Optional<AttendanceSession> findActiveSessionForSection(@Param("sectionId") Long sectionId);

    // ── Section-wide total conducted (for admin/HOD dashboards) ───────────────
    // Counts non-cancelled sessions (ENDED + ACTIVE) for a section
    @Query("""
                SELECT COUNT(s) FROM AttendanceSession s
                WHERE s.section.id = :sectionId
                AND s.status <> 'CANCELLED'
            """)
    long countConductedBySectionId(@Param("sectionId") Long sectionId);

    // Legacy name kept so existing service code doesn't break immediately
    long countBySectionId(Long sectionId);

    // ── Per-subject: sessions conducted for a specific subject in a section ────
    // Counts ENDED + ACTIVE sessions tagged with this subject (excludes CANCELLED).
    // Includes sessions where the section was merged-in (join table).
    @Query("""
                SELECT COUNT(DISTINCT s.id) FROM AttendanceSession s
                WHERE s.subject.id = :subjectId
                AND s.status <> 'CANCELLED'
                AND (
                    s.section.id = :sectionId
                    OR EXISTS (
                        SELECT 1 FROM AttendanceSessionSection ass
                        WHERE ass.session = s AND ass.section.id = :sectionId
                    )
                )
            """)
    long countConductedBySubjectIdAndSectionId(
            @Param("subjectId") Long subjectId,
            @Param("sectionId") Long sectionId);

    // Legacy name for existing service code
    @Query("SELECT COUNT(s) FROM AttendanceSession s " +
            "WHERE s.subject.id = :subjectId " +
            "AND s.section.id  = :sectionId")
    long countBySubjectIdAndSectionId(
            @Param("subjectId") Long subjectId,
            @Param("sectionId") Long sectionId);

    // ── Fallback: untagged sessions (legacy data without subject set) ──────────
    @Query("SELECT COUNT(s) FROM AttendanceSession s " +
            "WHERE s.faculty.id = :facultyId " +
            "AND s.section.id   = :sectionId " +
            "AND s.subject IS NULL")
    long countUntaggedByFacultyIdAndSectionId(
            @Param("facultyId") Long facultyId,
            @Param("sectionId") Long sectionId);

    // ── Faculty session history (newest first) ────────────────────────────────
    List<AttendanceSession> findByFacultyIdOrderByStartTimeDesc(Long facultyId);

    @Query("SELECT s FROM AttendanceSession s " +
            "WHERE s.faculty.id = :facultyId " +
            "AND (cast(:startDate as localdatetime) IS NULL OR s.startTime >= :startDate) " +
            "AND (cast(:endDate as localdatetime) IS NULL OR s.startTime <= :endDate) " +
            "ORDER BY s.startTime DESC")
    List<AttendanceSession> findByFacultyIdAndDateRange(
            @Param("facultyId") Long facultyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT s FROM AttendanceSession s " +
            "WHERE s.subject.id = :subjectId " +
            "AND s.status <> 'CANCELLED' " +
            "AND (cast(:startDate as localdatetime) IS NULL OR s.startTime >= :startDate) " +
            "AND (cast(:endDate as localdatetime) IS NULL OR s.startTime <= :endDate) " +
            "AND (s.section.id = :sectionId OR EXISTS (" +
            "    SELECT 1 FROM AttendanceSessionSection ass " +
            "    WHERE ass.session = s AND ass.section.id = :sectionId" +
            ")) " +
            "ORDER BY s.startTime DESC")
    List<AttendanceSession> findSessionsForDebarredList(
            @Param("subjectId") Long subjectId,
            @Param("sectionId") Long sectionId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

}
