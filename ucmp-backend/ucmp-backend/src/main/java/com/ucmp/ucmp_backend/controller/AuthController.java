package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.model.RegisterRequest;

import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.config.JwtUtil;
import com.ucmp.ucmp_backend.service.StudentService;
import com.ucmp.ucmp_backend.service.UserService;
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
    public String hello() {
        return "AuthCont.. is working !!";
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("Inside register controller");
        return userService.register(request);
    }

    }