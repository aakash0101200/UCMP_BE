package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.dto.ProfileUpdateRequest;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

    @Transactional
    public ProfileResponse updateProfile(String collegeId, ProfileUpdateRequest request) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser_CollegeId(user.getCollegeId());
            profile.setName(user.getName());
            profile.setEmail(user.getEmail());
            profile.setUser(user);
            profile = profileRepository.save(profile);
            user.setProfile(profile);
        }

        // Update profile fields (Name is read-only per security feedback)
        if (request.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }

        userRepository.save(user); // Save the user to cascade changes to profile

        // Map the updated entities to the response DTO
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        // Build student info with IDs (if the user is a student)
        ProfileResponse.StudentInfo studentInfo = null;
        Student student = studentRepository.findByUser(user).orElse(null);
        if (student != null) {
            studentInfo = ProfileResponse.StudentInfo.builder()
                    .rollNumber(student.getRollNumber())
                    .year(student.getYear())
                    .batchId(student.getBatch() != null ? student.getBatch().getId() : null)
                    .batchName(student.getBatch() != null ? student.getBatch().getBatchName() : null)
                    .sectionId(student.getSection() != null ? student.getSection().getId() : null)
                    .sectionName(student.getSection() != null ? student.getSection().getSectionName() : null)
                    .build();
        }

        // Build faculty info with IDs (if the user is a faculty)
        ProfileResponse.FacultyInfo facultyInfo = null;
        Faculty faculty = facultyRepository.findByUser(user).orElse(null);
        if (faculty != null) {
            facultyInfo = ProfileResponse.FacultyInfo.builder()
                    .facultyId(faculty.getId())
                    .department(faculty.getDepartment())
                    .designation(faculty.getDesignation())
                    .officeLocation(faculty.getOfficeLocation())
                    .officeHours(faculty.getOfficeHours())
                    .build();
        }

        return ProfileResponse.builder()
                .collegeId(user.getCollegeId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .phoneNumber(profile.getPhoneNumber())
                .address(profile.getAddress())
                .roles(roleNames)
                .department(user.getDepartment())
                .yearScope(user.getYearScope())
                .student(studentInfo)
                .faculty(facultyInfo)
                .build();
    }
}
