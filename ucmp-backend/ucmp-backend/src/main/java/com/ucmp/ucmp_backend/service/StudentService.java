package com.ucmp.ucmp_backend.service;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> getAll(){
        return studentRepository.findAll();
    }

    public Student addStudent(Student student){
        return studentRepository.save(student);
    }

    public Student updateStudent(String id,Student student){
        student.setCollegeId(id);
        return studentRepository.save(student);
    }
}
