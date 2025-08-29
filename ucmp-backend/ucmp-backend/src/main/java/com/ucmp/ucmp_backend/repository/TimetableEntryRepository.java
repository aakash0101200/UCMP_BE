//package com.ucmp.ucmp_backend.repository;
//
//import com.ucmp.ucmp_backend.model.TimetableEntry;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.time.DayOfWeek;
//import java.util.List;
//
//public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
//    List<TimetableEntry> findByStudentIdOrderByDayAscStartTimeAsc(Long sectionId);
//    List<TimetableEntry> findByFacultyIdOrderByDayAscStartTimeAsc(Long facultyId);
//    List<TimetableEntry> findBySectionIdOrderByDayAscStartTimeAsc(Long sectionId, DayOfWeek day);
//}
