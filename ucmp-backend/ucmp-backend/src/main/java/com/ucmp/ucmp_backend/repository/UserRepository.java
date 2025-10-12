package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByCollegeId(String id);
    Optional<User> findByCollegeId(String collegeId);
    Optional<User> findByEmail(String email);
    void deleteByCollegeId(String collegeId);
}