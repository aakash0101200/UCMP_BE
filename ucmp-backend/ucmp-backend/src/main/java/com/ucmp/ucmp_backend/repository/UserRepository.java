package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByCollegeId(String collegeId);
}