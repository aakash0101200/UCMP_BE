//package com.ucmp.ucmp_backend.repository;
//
//import com.ucmp.ucmp_backend.model.Faculty;
//import com.ucmp.ucmp_backend.model.Schedule;
//import com.ucmp.ucmp_backend.model.Section;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.time.DayOfWeek;
//import java.util.List;
//
//public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
//    List<Schedule> findBySection(Section section);
//    List<Schedule> findByFaculty(Faculty faculty);
//
//    //find by section/faculty + day
//    List<Schedule> findBySectionAndDayOfWeek(Section section, DayOfWeek dayOfWeek);
//    List<Schedule> findByFacultyAndDayOfWeek(Faculty faculty, DayOfWeek dayOfWeek);
//
//}
