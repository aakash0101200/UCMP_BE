package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
//@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Injected from Security config

//    @Transactional
//    public User createStudent(StudentRegistrationRequest request) {
//        // Check if email or collegeId already exists
//        if (userRepository.existsByEmailOrCollegeId(request.getEmail(), request.getCollegeId())) {
//            throw new RuntimeException("User with this email or ID already exists.");
//        }
//
//        String encryptedPassword = passwordEncoder.encode(request.getPassword());
//
//        // Find advisor if provided
//        Faculty advisor = null;
//        if (request.getFacultyAdvisorId() != null) {
//            // This fetch could be optimized, but for clarity:
//            advisor = (Faculty) userRepository.findById(request.getFacultyAdvisorId())
//                    .orElseThrow(() -> new RuntimeException("Advisor not found"));
//            // Ensure the found user is actually a Faculty
//            if (advisor.getRole() != User.Role.FACULTY) {
//                throw new RuntimeException("The provided advisor ID is not a faculty member.");
//            }
//        }
//
//        Student newStudent = new Student(
//                request.getCollegeId(),
//                encryptedPassword,
//                request.getName(),
//                request.getEmail(),
//                request.getMajor(),
//                request.getEnrollmentYear()
//        );
//        newStudent.setFacultyAdvisor(advisor);
//
//        return userRepository.save(newStudent);
//    }
//
//    @Transactional
//    public User createFaculty(FacultyRegistrationRequest request) {
//        if (userRepository.existsByEmailOrCollegeId(request.getEmail(), request.getCollegeId())) {
//            throw new RuntimeException("User with this email or ID already exists.");
//        }
//
//        String encryptedPassword = passwordEncoder.encode(request.getPassword());
//
//        Faculty newFaculty = new Faculty(
//                request.getCollegeId(),
//                encryptedPassword,
//                request.getName(),
//                request.getEmail(),
//                request.getDepartment(),
//                request.getOfficeLocation()
//        );
//        // officeHours can be set separately if not in constructor
//        newFaculty.setOfficeHours(request.getOfficeHours());
//
//        return userRepository.save(newFaculty);
//    }
//
//    // Similar method for createAdmin...
//    // User finder methods...
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
