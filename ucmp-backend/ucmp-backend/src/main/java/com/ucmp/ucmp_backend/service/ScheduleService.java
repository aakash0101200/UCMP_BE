//package com.ucmp.ucmp_backend.service;
//
//import com.ucmp.ucmp_backend.dto.ScheduleSlotDTO;
//import com.ucmp.ucmp_backend.model.Role;
//import com.ucmp.ucmp_backend.model.Student;
//import com.ucmp.ucmp_backend.model.User;
//import com.ucmp.ucmp_backend.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//
//public class ScheduleService {
//    private final UserRepository userRepository;
//    private final StudentRepository studentRepository;
//    private final FacultyRepository facultyRepository;
//    private final SectionRepository sectionRepository;
//    private final ScheduleRepository scheduleRepository;
//    private final TimetableEntryRepository timetableEntryRepository;
//
//    //--- Queries
//
//    public List<ScheduleSlotDTO> getMySchedule(){
//        User me = currentUser();
//        if(me.getRole() == Role.STUDENT){
//            Student st = studentRepository.findByCollegeId(me.getId())
//                    .orElseThrow(()-> new RuntimeException("Student profile not found"));
//        }
//    }
//}
