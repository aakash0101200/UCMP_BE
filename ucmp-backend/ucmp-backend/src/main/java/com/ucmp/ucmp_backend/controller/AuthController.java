package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.model.RegisterRequest;

import com.ucmp.ucmp_backend.repository.StudentRepository;
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

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String hello(){
        return "hello, world!!";
    }


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
        return userService.register(request);
    }

    }