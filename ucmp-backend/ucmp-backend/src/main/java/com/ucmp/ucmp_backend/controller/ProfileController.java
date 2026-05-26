package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.dto.ProfileUpdateRequest;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ProfileRepository profileRepository;

    /**
     * Retrieves the profile of the currently authenticated user.
     * Now includes sectionId/batchId for students and facultyId for faculty,
     * which the frontend schedule views need to call the timetable API.
     */
    @GetMapping
    @Transactional
    public ResponseEntity<ProfileResponse> getProfile(
            Authentication authentication,
            @RequestParam(required = false) String collegeId) {

        String targetCollegeId = (collegeId != null && !collegeId.trim().isEmpty()) ? collegeId
                : authentication.getName();
        User user = userRepository.findByCollegeId(targetCollegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Profile profile = user.getProfile();
        if (profile == null) {
            // Create profile on-the-fly for safety and auto-link missing records
            profile = new Profile();
            profile.setUser_CollegeId(user.getCollegeId());
            profile.setName(user.getName());
            profile.setEmail(user.getEmail());
            profile.setUser(user);
            profile = profileRepository.save(profile);
            user.setProfile(profile);
            userRepository.save(user);
        }

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

        ProfileResponse response = ProfileResponse.builder()
                .collegeId(user.getCollegeId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .roles(roleNames)
                .department(user.getDepartment())
                .yearScope(user.getYearScope())
                .student(studentInfo)
                .faculty(facultyInfo)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the profile of the currently authenticated user.
     */
    @PutMapping("/update")
    public ResponseEntity<ProfileResponse> updateProfile(Authentication authentication,
            @RequestBody ProfileUpdateRequest request) {
        String collegeId = authentication.getName();
        ProfileResponse updatedProfile = profileService.updateProfile(collegeId, request);
        return ResponseEntity.ok(updatedProfile);
    }
}
