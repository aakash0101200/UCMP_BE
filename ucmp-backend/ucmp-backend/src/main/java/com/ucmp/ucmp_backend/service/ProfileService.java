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

    @Transactional
    public ProfileResponse updateProfile(String collegeId, ProfileUpdateRequest request) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Profile profile = user.getProfile();
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found for user: " + collegeId);
        }

        // Update profile fields
        if (request.getName() != null) {
            user.setName(request.getName());
            profile.setName(request.getName());
        }
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
//        profileRepository.save(profile);

        // Map the updated entities to the response DTO
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return ProfileResponse.builder()
                .collegeId(user.getCollegeId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .roles(roleNames)
                .build();
    }
}
