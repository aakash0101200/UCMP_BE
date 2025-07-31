package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.config.JwtUtil;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
//    @Autowired
//    private FacultyRepository facultyRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ProfileRepository profileRepository;
//    @Autowired private AdminRepository adminRepository;

    public ResponseEntity<?> login(LoginRequest request) {

        // 1. Find user by collegeId by request.getId()
        Optional<User> userOpt = userRepository.findByCollegeId(request.getId());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }


        User user = userOpt.get();

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid password");
        }

        // 3. Verify role matches
        if (!user.getRole().name().equals(request.getRole())) {
            return ResponseEntity.status(403).body("User does not have the " + request.getRole() + " role");
        }

        // 4. Generate JWT token
        String token = jwtUtil.generateToken(user.getCollegeId(), user.getRole().name());

        // 5. Return response
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name()));
    }



   //     Create or Update a profile

    public User saveUser(User user) {
        return userRepository.save(user);
    }



    @Transactional
    public ResponseEntity<?> register(RegisterRequest request) {

        Optional<User> existing = userRepository.findByCollegeId(request.getCollegeId());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("User already exists with this College ID");
        }

        if (request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body("Password too short");
        }



        String collegeId = request.getCollegeId().trim();
        String name = request.getName().trim();
        String email = request.getEmail().trim();
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        String role = request.getRole().trim().toUpperCase();

        User.Role userRole = User.Role.valueOf(role);



        User user = new User(
                collegeId,
                hashedPassword,
                email,
                name,
                userRole
        );
        userRepository.save(user);

        Profile profile = new Profile();
        profile.setCollegeId(collegeId);
        profile.setEmail(email);
        profile.setName(name);
        profileRepository.save(profile);

        if(userRole.equals(User.Role.STUDENT)){
            Student student = new Student();
            student.setCollegeId(collegeId);
            student.setProfile(profile);
            studentRepository.save(student);
        }



        return ResponseEntity.ok("Registration successful");
    }


}

////List all profiles/users
//public List<User> getAll(){
//    return UserRepository.findAll();
//}


//@Component
//public class CurrentUserMapper {
//
//    @Autowired private StudentRepository studentRepo;
//    @Autowired private FacultyRepository facultyRepo;
//    @Autowired private AdminRepository adminRepo;
//
//    public Object getCurrentUserEntity(String email, String role) {
//        return switch (role.toUpperCase()) {
//            case "STUDENT" -> studentRepo.findByEmail(email).orElseThrow();
//            case "FACULTY" -> facultyRepo.findByEmail(email).orElseThrow();
//            case "ADMIN" -> adminRepo.findByEmail(email).orElseThrow();
//            default -> throw new IllegalArgumentException("Unknown role: " + role);
//        };
//    }
//}
//Then in any controller or service:
//
//java
//        Copy
//Edit
//String email = SecurityContextHolder.getContext().getAuthentication().getName();
//String role = jwtUtil.getRoleFromToken(token);
//Object userEntity = currentUserMapper.getCurrentUserEntity(email, role);
//
//Option 2: Abstract Superclass + Polymorphism (Advanced)
//You can define a base UserProfile interface or abstract class and let Student, Faculty, and Admin implement/extend it.
//
//Then:
//
//java
//        Copy
//Edit
//public interface UserProfile {
//    String getEmail();
//    String getRole();
//}
//
//public class Student implements UserProfile { ... }
//public class Faculty implements UserProfile { ... }
//
//public UserProfile resolveUser(String email, String role) {
//    // Return appropriate object as UserProfile
//}
//🔐 SecurityConfig Tips
//In your SecurityConfig.java, configure role-based endpoints:
//
//java
//        Copy
//Edit
//        .authorizeHttpRequests()
//    .requestMatchers("/api/v1/students/**").hasRole("STUDENT")
//    .requestMatchers("/api/v1/faculty/**").hasRole("FACULTY")
//    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
//    .anyRequest().authenticated()
