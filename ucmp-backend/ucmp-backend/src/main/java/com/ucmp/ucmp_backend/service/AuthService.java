//list or set
package com.ucmp.ucmp_backend.service;
import com.ucmp.ucmp_backend.dto.LoginRequest;
import com.ucmp.ucmp_backend.dto.LoginResponse;
import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.dto.RegisterRequest;
import com.ucmp.ucmp_backend.model.Role;

import com.ucmp.ucmp_backend.config.JwtUtil;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.lang.model.element.Name;
import java.util.Collections;
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
    @Autowired
    private final JwtUtil jwtUtil;

    // Register new user
    public AuthResponse register(@Valid RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByCollegeId(request.getCollegeId())) {
            throw new RuntimeException("CollegeId already exists");
        }

        // Create User
        User user = User.builder()
                .collegeId(request.getCollegeId())
                .name(request.getName())

                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole().toString())) // convert string → enum
                .build();

        userRepository.save(user);

        //create a profile record

        Profile profile = new Profile();
        profile.setCollegeId(user.getCollegeId());
        profile.setName(user.getName());
        profile.setEmail(user.getEmail());
        profile.setUser(user);
        //link profile <- user
        user.setProfile(profile);

        //if role is student -> create student
        if(user.getRole() == Role.STUDENT) {
            Student student = new Student();
            student.setUser(user);

//            profile = profileRepository.getProfilesByCollegeId(user.getCollegeId())
//                            .orElseThrow(() -> new RuntimeException("Profile Not Found"));
//            //student.setProfile(profile); //link to profile

            student.setRollNumber(request.getRollNumber());
//            student.setBatch(request.getBranch());
            student.setYear(request.getYear());
            student.setCollegeId(user.getCollegeId());
            Batch batch = batchRepository.findByBatchName(request.getBranch())
                    .orElseGet(() -> {
                        Batch newBatch = new Batch();
                        newBatch.setBatchName(request.getBranch());
                        return batchRepository.save(newBatch);
                    });
            student.setBatch(batch);
            studentRepository.save(student);
            System.out.println("student registered successfully");
        }


        //if role is faculty -> create faculty
        if(user.getRole() == Role.FACULTY) {
            Faculty faculty = new Faculty();
            faculty.setUser(user);
//          //  faculty.setProfile(profile);

            faculty.setCollegeId(user.getCollegeId());
            faculty.setDepartment(request.getDepartment());
            faculty.setDesignation(request.getDesignation());
            facultyRepository.save(faculty);
            System.out.println("Faculty registered successfully");

        }

        String token = jwtUtil.generateToken(user.getCollegeId(), user.getRole().name());
        return new AuthResponse(user.getCollegeId(), user.getRole().name());
    }

    //  Login existing user
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByCollegeId(request.getCollegeId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
//   Convert roles
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getCollegeId(), roleNames);


        Profile profile = profileRepository.getProfilesByCollegeId(user.getCollegeId())
                .orElse(null);
        ProfileResponse profileResponse = profile != null
                ? ProfileResponse.builder()
                .name(profile.getName())
                .email(profile.getEmail())
                .roles(Collections.singletonList(String.join(",", roleNames)))
//                .department(user.getDepaerment())
//                .designation(user.getDesignation())
                .collegeId(user.getCollegeId())
                .build()
                : ProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .roles((List<String>) roleNames)
                .collegeId(user.getCollegeId())
                .build();


        return LoginResponse.builder()
                .token(token)
                .profile(profileResponse)
                .build();
    }        //return new AuthResponse(user.getCollegeId(), user.getRole().name());

}
