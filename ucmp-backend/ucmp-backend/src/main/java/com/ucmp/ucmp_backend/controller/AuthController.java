package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.config.JwtUtil;
import com.ucmp.ucmp_backend.dto.LoginRequest;
import com.ucmp.ucmp_backend.dto.LoginResponse;
import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.dto.RegisterRequest;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.service.AuthService;

import com.ucmp.ucmp_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserService userService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @GetMapping
    public String hello() {
        return "AuthCont.. is working !!";
    }


    @PostMapping("/login")

    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try{
            LoginResponse response = authService.login(request);

            return ResponseEntity.ok(response);
        } catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try{
            authService.register(request);
            return ResponseEntity.ok("User Register Successfully");
        } catch (RuntimeException exception){
            return  ResponseEntity.badRequest().body(null);
        }
    }

    }