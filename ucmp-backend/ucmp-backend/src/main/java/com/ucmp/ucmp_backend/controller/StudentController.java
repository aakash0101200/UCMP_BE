package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {


    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService){
        this.studentService = studentService;
    }

    @GetMapping("/")
    public List<Student> getAll(){
        return studentService.getAll();
    }

    @PostMapping("/{id}")
    public Student addStudent(@RequestBody Student student){
        return studentService.addStudent(student);
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable String id,@RequestBody Student student){
        return studentService.updateStudent(id, student);
    }




}
