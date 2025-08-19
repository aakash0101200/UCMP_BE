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
    

//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private FacultyRepository facultyRepository;

//    @Autowired private PasswordEncoder passwordEncoder;
//    @Autowired private JwtUtil jwtUtil;
//    @Autowired private StudentRepository studentRepository;
//    @Autowired private ProfileRepository profileRepository;
//    @Autowired private AdminRepository adminRepository;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       StudentRepository studentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }


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





//    @Transactional
    public ResponseEntity<String> register(RegisterRequest request) {

        // 1. Check if user already exists
        if (userRepository.findByCollegeId(request.getCollegeId().trim()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists with this College ID");
        }

        // 2. Validate password length
        if (request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }

        // 3. Parse and validate role
        User.Role userRole;
        try {
            userRole = User.Role.valueOf(request.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid role: " + request.getRole());
        }


        // 4. Create User entity
        User user = new User(
                request.getCollegeId().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getName().trim(),
                request.getEmail().trim(),
                userRole
        );
        userRepository.save(user);

        //for checking
//        try{
//            userRepository.save(user);
//
//        }catch (Exception e) {
//            System.out.println(e);
//
//            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
//        }


        // 5. Create Profile entity
//        Profile profile = new Profile();
//        profile.setCollegeId(user.getCollegeId());
//        profile.setName(user.getName());
//        profile.setEmail(user.getEmail());
//        profileRepository.save(profile);
//
//
//        // 6. Persist Profile and, if student, Student with cascade
//        if (userRole == User.Role.STUDENT) {
//            Student student = new Student();
//            student.setCollegeId(user.getCollegeId());
//            student.setProfile(profile);
//            profile.setStudent(student);
//            try {
//                studentRepository.save(student); // Cascades profile save
//            }catch(Exception ex) {
//                return ResponseEntity.badRequest().body(ex.getMessage());
//            }
//        }
        return ResponseEntity.ok("Registration successful");
    }




    //     Create or Update a profile

    public User saveUser(User user) {
        return userRepository.save(user);
    }

}
