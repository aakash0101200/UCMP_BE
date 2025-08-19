package com.ucmp.ucmp_backend.service;
import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
import com.ucmp.ucmp_backend.mapper.StudentProfileMapper;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class StudentService {

    private final StudentRepository studentRepository;
    private final ProfileRepository profileRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository, ProfileRepository profileRepository) {

        this.studentRepository = studentRepository;
        this.profileRepository = profileRepository;
    }

    public StudentProfileDTO createStudentProfile(StudentProfileDTO dto) {
        Profile profile = new Profile();
        profile.setName(dto.getName());
        profile.setEmail(dto.getEmail());
        Profile savedProfile = profileRepository.save(profile);


        Student student=new Student();
        student.setCollegeId(dto.getCollegeId());
        student.setProfile(savedProfile);
//        StudentProfileMapper.updateEntitiesFromDto(dto,student,profile);
        Student savedStudent = studentRepository.save(student);

        return StudentProfileMapper.toDto(savedStudent);
    }

    public Optional<StudentProfileDTO> updateStudentProfile(String collegeId,StudentProfileDTO dto) {

        return studentRepository.findByCollegeId(collegeId).map(
                student->{
                    Profile profile = student.getProfile();
                    StudentProfileMapper.updateEntitiesFromDto(dto,student,profile);
                    profileRepository.save(profile);
                    studentRepository.save(student);
                    return StudentProfileMapper.toDto(student);
                }
        );
    }

    public Optional<StudentProfileDTO> getStudentProfileByCollegeId(String id) {
        return studentRepository.findById(id).map(StudentProfileMapper::toDto);
    }




    public void deleteStudentByCollegeId(String collegeId, boolean alsoDeleteProfile) {
        studentRepository.findByCollegeId(collegeId).ifPresent(student -> {
            Long profileId = student.getProfile().getProfileId();
            studentRepository.deleteById(collegeId);
            if (alsoDeleteProfile) {
                profileRepository.deleteById(profileId);
            }
        });
    }
}
