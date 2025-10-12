package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.dto.ProfileUpdateRequest;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    /**
     * Retrieves the profile of the currently authenticated user.
     * The collegeId is securely extracted from the authentication context.
     * @param authentication The Spring Security Authentication object.
     * @return A ResponseEntity containing the user's profile information.
     */
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        String collegeId = authentication.getName();
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getProfile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found for user: " + collegeId);
        }

        // Map the User and Profile entities to the DTO
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        ProfileResponse response = ProfileResponse.builder()
                .collegeId(user.getCollegeId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfile().getProfilePictureUrl())
                .roles(roleNames)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the profile of the currently authenticated user.
     * @param authentication The Spring Security Authentication object.
     * @param request The DTO containing the profile update data.
     * @return The updated ProfileResponse.
     */
    @PutMapping("/update")
    public ResponseEntity<ProfileResponse> updateProfile(Authentication authentication,
                                                         @RequestBody ProfileUpdateRequest request) {
        String collegeId = authentication.getName();
        ProfileResponse updatedProfile = profileService.updateProfile(collegeId, request);
        return ResponseEntity.ok(updatedProfile);
    }

}
