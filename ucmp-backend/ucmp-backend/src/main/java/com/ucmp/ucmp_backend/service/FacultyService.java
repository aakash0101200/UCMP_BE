package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import org.hibernate.annotations.processing.Find;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FacultyService {
    private final FacultyRepository facultyRepository;
    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    public Faculty saveFaculty(Faculty faculty) {
        return facultyRepository.save(faculty);
    }
    public List<Faculty> getAllFaculties() {
        return  facultyRepository.findAll();
    }
    public Optional<Faculty> getFacultyById(Long id) {
        return facultyRepository.findById(id);
    }
    public Optional<Faculty> findByCollegeId(String collegeId) {
        return facultyRepository.findByCollegeId(collegeId);
    }
    public  Optional<Faculty> findByDepartment(String department) {
        return facultyRepository.findByDepartment(department);
    }

    public void deleteFaculty(Long id) {
        facultyRepository.deleteById(id);
    }
}
