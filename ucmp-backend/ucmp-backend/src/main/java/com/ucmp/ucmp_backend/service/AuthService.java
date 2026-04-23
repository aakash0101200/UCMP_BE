package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.config.JwtUtil;
import com.ucmp.ucmp_backend.dto.*;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ProfileRepository profileRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;
    private final SectionRepository sectionRepository;
    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;

    // Register a new user (Now restricted to Students only)
    public LoginResponse register(RegisterRequest request) {
        // 🚨 NEW SECURITY CHECK: Prevent public registration for Faculty/Admin
        boolean attemptsToRegisterAsStaff = request.getRoles().stream()
                .anyMatch(role -> role == RoleName.ADMIN || role == RoleName.FACULTY);

        if (attemptsToRegisterAsStaff) {
            throw new RuntimeException("Security Violation: Public registration is restricted to Students only.");
        }

        // ... Keep all your existing checks below this line ...
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByCollegeId(request.getCollegeId())) {
            throw new RuntimeException("College ID already exists");
        }

        // Ensure role exists
        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());


        // Create user
        User user = User.builder()
                .collegeId(request.getCollegeId())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles) // Roles now correctly set
                .build();


        userRepository.save(user);

        // Create profile
        Profile profile = new Profile();
        profile.setUser_CollegeId(user.getCollegeId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setUser(user);
        user.setProfile(profile);
        profileRepository.save(profile);

        // Handle STUDENT-specific details
        if (roles.stream().anyMatch(r -> r.getName() == RoleName.STUDENT)) {
            Student student = new Student();
            student.setUser(user);
            student.setRollNumber(request.getRollNumber());
            student.setYear(String.valueOf(request.getYear()));
            student.setCollegeId(user.getCollegeId());

            Batch batch = batchRepository.findByBatchName(request.getBranch())
                    .orElseGet(() -> {
                        Batch newBatch = new Batch();
                        newBatch.setBatchName(request.getBranch());
                        return batchRepository.save(newBatch);
                    });
            student.setBatch(batch);
            studentRepository.save(student);
        }

        // Handle FACULTY-specific details
        if (roles.stream().anyMatch(r -> r.getName() == RoleName.FACULTY)) {
            Faculty faculty = new Faculty();
            faculty.setUser(user);
            faculty.setCollegeId(user.getCollegeId());
            faculty.setDepartment(request.getDepartment());
            faculty.setDesignation(request.getDesignation());

            // NEW LOGIC: Fetch and attach sections
            if (request.getSectionIds() != null && !request.getSectionIds().isEmpty()) {
                List<Section> assignedSections = sectionRepository.findAllById(request.getSectionIds());
                faculty.setSections(new HashSet<>(assignedSections));
            }

            facultyRepository.save(faculty);
        }


        // Generate JWT
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getCollegeId(), roleNames);

        // Build profile response
        ProfileResponse profileResponse = ProfileResponse.builder()
                .name(profile.getName())
                .email(profile.getEmail())
                .roles(List.copyOf(roleNames))
                .collegeId(user.getCollegeId())
                .build();

        return LoginResponse.builder()
                .token(token)
                .profile(profileResponse)
                .build();
    }

    @Transactional
    public void adminCreateFaculty(AdminCreateFacultyRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByCollegeId(request.getCollegeId())) {
            throw new RuntimeException("College ID already exists");
        }

        Role facultyRole = roleRepository.findByName(RoleName.FACULTY)
                .orElseThrow(() -> new RuntimeException("FACULTY role not found"));

        // 1. Create User with default password
        User user = User.builder()
                .collegeId(request.getCollegeId())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode("Faculty@123")) // Default password
                .roles(Collections.singleton(facultyRole))
                .build();
        userRepository.save(user);

        // 2. Create Profile
        Profile profile = new Profile();
        profile.setUser_CollegeId(user.getCollegeId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setUser(user);
        profileRepository.save(profile);

        // 3. Create Faculty & Map Sections
        Faculty faculty = new Faculty();
        faculty.setUser(user);
        faculty.setCollegeId(user.getCollegeId());
        faculty.setDepartment(request.getDepartment());
        faculty.setDesignation(request.getDesignation());

        if (request.getSectionIds() != null && !request.getSectionIds().isEmpty()) {
            List<Section> assignedSections = sectionRepository.findAllById(request.getSectionIds());
            faculty.setSections(new HashSet<>(assignedSections));
        }
        facultyRepository.save(faculty);
    }

    @Transactional
    public void adminCreateStudent(AdminCreateStudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByCollegeId(request.getCollegeId())) {
            throw new RuntimeException("College ID already exists");
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("STUDENT role not found"));

        // 1. Create User with default password
        User user = User.builder()
                .collegeId(request.getCollegeId())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode("Student@123")) // Default password
                .roles(Collections.singleton(studentRole))
                .build();
        userRepository.save(user);

        // 2. Create Profile
        Profile profile = new Profile();
        profile.setUser_CollegeId(user.getCollegeId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setUser(user);
        profileRepository.save(profile);

        // 3. Fetch Batch and Section
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found"));

        // 4. Create Student and Map to Batch & Section
        Student student = new Student();
        student.setUser(user);
        student.setCollegeId(user.getCollegeId());
        student.setRollNumber(request.getRollNumber());
        student.setYear(String.valueOf(request.getYear()));
        student.setBatch(batch);
        student.setSection(section);

        studentRepository.save(student);
    }

    // Login user
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByCollegeId(request.getCollegeId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getCollegeId(), roleNames);

        Profile profile = profileRepository.findByUser_CollegeId(user.getCollegeId())
                .orElse(null);

        ProfileResponse profileResponse = (profile != null)
                ? ProfileResponse.builder()
                .name(profile.getName())
                .email(profile.getEmail())
                .roles(List.copyOf(roleNames))
                .collegeId(user.getCollegeId())
                .build()
                : ProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .roles(List.copyOf(roleNames))
                .collegeId(user.getCollegeId())
                .build();

        return LoginResponse.builder()
                .token(token)
                .profile(profileResponse)
                .build();
    }
}