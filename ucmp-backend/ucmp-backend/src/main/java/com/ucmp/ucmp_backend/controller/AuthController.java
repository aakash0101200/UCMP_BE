package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.model.RegisterRequest;

import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.config.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping
    public String hello(){
        return "hello, world!!";
    }

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

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



    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

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

        boolean validRole = false;
        for (User.Role r : User.Role.values()) {
            if (r.name().equalsIgnoreCase(request.getRole())) {
                validRole = true;
                break;
            }
        }
        if (!validRole) {
            return ResponseEntity.badRequest().body("Invalid role selected");
        }



        User user = new User(
                collegeId,
                hashedPassword,
                email,
                name,
                User.Role.valueOf(role)
        );
        userRepository.save(user);

        return ResponseEntity.ok("Registration successful");
    }

}