package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.ProfileResponse;
import com.ucmp.ucmp_backend.dto.ProfileUpdateRequest;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.service.ProfileService;
import com.ucmp.ucmp_backend.service.StudentService;
import com.ucmp.ucmp_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Profile> getProfile(@RequestHeader("collegeId") String collegeId) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Profile profile = user.getProfile();
        if(profile == null) {
            return ResponseEntity.notFound().build();
        }
        ProfileResponse response = new ProfileResponse(
                profile.getCollegeId(),
                profile.getName(),
                profile.getEmail(),
                profile.getProfilePictureUrl(),
                user.getRole().name()
        );
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/update")
    public Profile updateProfile(@RequestParam String collegeId,
                                 @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(collegeId, request);
    }

}
