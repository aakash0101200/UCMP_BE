//package com.ucmp.ucmp_backend.mapper;
//
//import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
//import com.ucmp.ucmp_backend.model.Profile;
//import com.ucmp.ucmp_backend.model.Student;
//
//public class StudentProfileMapper {
//
//    public static StudentProfileDTO toDto(Student student){
//        if(student == null){
//            return null;
//        }
//
//        Profile profile = student.getProfile();
//        StudentProfileDTO dto = new StudentProfileDTO();
//        dto.setCollegeId(student.getCollegeId());
//
//        if(profile != null){
//            dto.setProfileId(profile.getProfileId());
//            dto.setName(profile.getName());
//            dto.setEmail(profile.getEmail());
//
//        }
//
//        return dto;
//    }
//
//    public static void updateEntitiesFromDto(StudentProfileDTO dto, Student student,Profile profile){
//
////        student.setCollegeId(dto.getCollegeId());
//
////        profile.setProfileId(dto.getProfileId());
//        if(dto.getName()!=null){
//            profile.setName(dto.getName());
//
//        }
//        if(dto.getEmail()!=null){
//            profile.setEmail(dto.getEmail());
//
//
//        }
//    }
//
//}
