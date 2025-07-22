package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    // This class will contain methods for user management, such as registration, login, and role management.
    // It will interact with the UserRepository to perform CRUD operations on User entities.

    // Example method to find a user by college ID
    // public Optional<User> findUserByCollegeId(String collegeId) {
    //     return userRepository.findByCollegeId(collegeId);
    // }

    // Additional methods for user registration, authentication, etc. can be added here.
    @Autowired
    private UserRepository userRepository;

    public Optional<User> getUserByCollegeId(String collegeId) {
        return userRepository.findByCollegeId(collegeId);
    }
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
