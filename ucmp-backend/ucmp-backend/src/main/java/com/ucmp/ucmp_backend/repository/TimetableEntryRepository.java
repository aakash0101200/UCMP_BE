package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    // --- Student / Section views ---
    List<TimetableEntry> findBySectionIdAndAcademicTermOrderByDayAscStartTimeAsc(
        Long sectionId, String academicTerm
    );
    List<TimetableEntry> findBySectionIdAndAcademicTermAndDay(
        Long sectionId, String academicTerm, DayOfWeek day
    );

    // --- Faculty views ---
    List<TimetableEntry> findByFacultyIdAndAcademicTermOrderByDayAscStartTimeAsc(
        Long facultyId, String academicTerm
    );

    // --- Room views ---
    List<TimetableEntry> findByRoomIdAndAcademicTermOrderByDayAscStartTimeAsc(
        Long roomId, String academicTerm
    );

    // --- Conflict detection queries ---

    // Check: Is this faculty already teaching at this day+time in this term?
    boolean existsByFacultyIdAndDayAndStartTimeAndAcademicTerm(
        Long facultyId, DayOfWeek day, LocalTime startTime, String academicTerm
    );

    // Check: Is this section already in class at this day+time in this term?
    boolean existsBySectionIdAndDayAndStartTimeAndAcademicTerm(
        Long sectionId, DayOfWeek day, LocalTime startTime, String academicTerm
    );

    // Check: Is this room already occupied at this day+time in this term?
    boolean existsByRoomIdAndDayAndStartTimeAndAcademicTerm(
        Long roomId, DayOfWeek day, LocalTime startTime, String academicTerm
    );

    // Get all entries for a faculty on a specific day (to detect consecutive-hour overload)
    List<TimetableEntry> findByFacultyIdAndDayAndAcademicTermOrderByStartTimeAsc(
        Long facultyId, DayOfWeek day, String academicTerm
    );

    // Delete all entries for a term (used during timetable regeneration)
    void deleteByAcademicTerm(String academicTerm);

    // Count how many slots a faculty has in a term (for overload detection)
    long countByFacultyIdAndAcademicTerm(Long facultyId, String academicTerm);
}
