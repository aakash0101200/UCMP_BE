package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.TimetableOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimetableOverrideRepository extends JpaRepository<TimetableOverride, Long> {

    @Query("SELECT o FROM TimetableOverride o WHERE o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findActiveOverridesByDate(@Param("date") LocalDate date);

    @Query("SELECT o FROM TimetableOverride o JOIN o.sections s WHERE s.id = :sectionId AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findActiveOverridesBySectionAndDate(@Param("sectionId") Long sectionId, @Param("date") LocalDate date);

    @Query("SELECT o FROM TimetableOverride o WHERE (o.originalFaculty.id = :facultyId OR o.newFaculty.id = :facultyId) AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findActiveOverridesByFacultyAndDate(@Param("facultyId") Long facultyId, @Param("date") LocalDate date);

    @Query("SELECT o FROM TimetableOverride o WHERE (o.originalRoom.id = :roomId OR o.newRoom.id = :roomId) AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findActiveOverridesByRoomAndDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);

    @Query("SELECT o FROM TimetableOverride o WHERE o.timetableEntry.id = :entryId AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findActiveOverridesByEntryAndDate(@Param("entryId") Long entryId, @Param("date") LocalDate date);

    @Query("SELECT o FROM TimetableOverride o WHERE o.newFaculty.id = :facultyId AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    List<TimetableOverride> findByNewFacultyIdAndOverrideDate(@Param("facultyId") Long facultyId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(o) FROM TimetableOverride o WHERE o.status = com.ucmp.ucmp_backend.model.OverrideStatus.PENDING AND (o.timetableEntry IS NULL OR o.timetableEntry.academicTerm = :term)")
    long countPendingByTerm(@Param("term") String term);

    @Query("SELECT COUNT(o) FROM TimetableOverride o WHERE o.overrideType = com.ucmp.ucmp_backend.model.OverrideType.CANCELLED AND o.status IN (com.ucmp.ucmp_backend.model.OverrideStatus.ACTIVE, com.ucmp.ucmp_backend.model.OverrideStatus.CONFIRMED, com.ucmp.ucmp_backend.model.OverrideStatus.COMPLETED) AND ((o.isRecurring = false AND o.overrideDate = :date) OR (o.isRecurring = true AND o.effectiveFrom <= :date AND (o.effectiveTo IS NULL OR o.effectiveTo >= :date)))")
    long countCancelledOverridesByDate(@Param("date") LocalDate date);
}
