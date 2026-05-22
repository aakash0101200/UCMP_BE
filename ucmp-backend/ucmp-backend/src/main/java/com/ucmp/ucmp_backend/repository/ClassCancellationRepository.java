package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.ClassCancellation;
import com.ucmp.ucmp_backend.model.CancellationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassCancellationRepository extends JpaRepository<ClassCancellation, Long> {

    // Check if a specific timetable slot is cancelled on a given date
    Optional<ClassCancellation> findByTimetableEntryIdAndCancellationDate(
            Long timetableEntryId, LocalDate cancellationDate);

    // All effective cancellations for a section on a given date
    // Used by schedule display — shows red "Cancelled" banners
    @Query("""
        SELECT cc FROM ClassCancellation cc
        WHERE cc.timetableEntry.section.id = :sectionId
        AND cc.cancellationDate = :date
        AND cc.approvalStatus IN ('AUTO_APPROVED', 'APPROVED')
    """)
    List<ClassCancellation> findEffectiveCancellationsForSectionOnDate(
            @Param("sectionId") Long sectionId,
            @Param("date") LocalDate date);

    // All pending cancellations awaiting HOD review (for HOD dashboard)
    List<ClassCancellation> findByApprovalStatus(CancellationStatus status);

    // All cancellations for a section this term (for reporting)
    @Query("""
        SELECT cc FROM ClassCancellation cc
        WHERE cc.timetableEntry.section.id = :sectionId
        AND cc.cancellationDate BETWEEN :from AND :to
        ORDER BY cc.cancellationDate DESC
    """)
    List<ClassCancellation> findBySectionAndDateRange(
            @Param("sectionId") Long sectionId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // All cancellations by a faculty member (for HOD audit)
    @Query("""
        SELECT cc FROM ClassCancellation cc
        WHERE cc.cancelledBy.id = :facultyId
        AND cc.cancellationDate BETWEEN :from AND :to
        ORDER BY cc.cancellationDate DESC
    """)
    List<ClassCancellation> findByFacultyAndDateRange(
            @Param("facultyId") Long facultyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT COUNT(cc) FROM ClassCancellation cc
        WHERE cc.cancellationDate = :date
        AND cc.approvalStatus IN ('AUTO_APPROVED', 'APPROVED')
    """)
    long countEffectiveCancellationsByDate(@Param("date") LocalDate date);
}
