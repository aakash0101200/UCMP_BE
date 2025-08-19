package com.ucmp.ucmp_backend.mapper;

import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.Student;

public class StudentProfileMapper {

    public static StudentProfileDTO toDto(Student student){
        if(student == null){
            return null;
        }

        Profile profile = student.getProfile();
        StudentProfileDTO studentProfileDTO = new StudentProfileDTO();
        studentProfileDTO.setCollegeId(student.getCollegeId());

        if(profile != null){
            studentProfileDTO.setProfileId(profile.getProfileId());
            studentProfileDTO.setName(profile.getName());
            studentProfileDTO.setEmail(profile.getEmail());

        }

        return studentProfileDTO;
    }

    public static void updateEntitiesFromDto(StudentProfileDTO studentProfileDTO, Student student,Profile profile){

        student.setCollegeId(studentProfileDTO.getCollegeId());

        profile.setProfileId(studentProfileDTO.getProfileId());
        profile.setName(studentProfileDTO.getName());
        profile.setEmail(studentProfileDTO.getEmail());
    }

}
