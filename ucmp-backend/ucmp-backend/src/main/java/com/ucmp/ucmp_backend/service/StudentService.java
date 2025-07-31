package com.ucmp.ucmp_backend.service;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class StudentService {

    private final StudentRepository studentRepository;
    private final ProfileRepository profileRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository, ProfileRepository profileRepository) {

        this.studentRepository = studentRepository;
        this.profileRepository = profileRepository;
    }

    public List<Student> getAll(){
        return studentRepository.findAll();
    }

    //Delete student

    public void deleteById(String id) {
        studentRepository.findByCollegeId(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
                studentRepository.deleteById(id);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }
}
