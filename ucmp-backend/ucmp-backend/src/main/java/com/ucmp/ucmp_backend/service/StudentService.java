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

    public StudentProfileDTO createStudentProfile(StudentProfileDTO studentProfileDTO) {
        Profile profile = new Profile();
        StudentProfileMapper.updateEntitiesFromDto(studentProfileDTO,null,profile);
        profile.setProfileId(studentProfileDTO.getProfileId());
        profileRepository.save(profile);

        Student student=new Student();
        student.setCollegeId(studentProfileDTO.getCollegeId());
        student.setProfile(profile);
        StudentProfileMapper.updateEntitiesFromDto(studentProfileDTO,student,profile);
        Student savedStudent = studentRepository.save(student);

        return StudentProfileMapper.toDto(student);
    }

    public Optional<StudentProfileDTO> updateStudentProfile(String collegeId,StudentProfileDTO studentProfileDTO) {
        Optional<Student> studentOpt =studentRepository.findById(collegeId);

        if(studentOpt.isEmpty()){ return Optional.empty(); }

        Student student = studentOpt.get();
        Profile profile = student.getProfile();
        StudentProfileMapper.updateEntitiesFromDto(studentProfileDTO,student,profile);
        profileRepository.save(profile);
        studentRepository.save(student);

        return Optional.of(StudentProfileMapper.toDto(student));
    }

    public Optional<StudentProfileDTO> getStudentProfileByCollegeId(String id) {
        return studentRepository.findById(id).map(StudentProfileMapper::toDto);
    }




    public void deleteStudentById(String id) {
        studentRepository.deleteById(id);
    }

}
