package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByCollegeId (String CollegeId);
//    Optional<Student> findByProfile_ProfileId(Long profileId);
    Optional<Student> findByRollNumber(String rollNumber);

}



