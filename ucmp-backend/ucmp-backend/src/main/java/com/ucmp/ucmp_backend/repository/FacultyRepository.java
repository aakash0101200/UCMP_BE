package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByCollegeId(String collegeId);
    Optional<Faculty> findByDepartment(String department);
}
