package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.service.ProfileService;
import com.ucmp.ucmp_backend.service.StudentService;
import com.ucmp.ucmp_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {


    private final ProfileService profileService;

    @Autowired
    public ProfileController ( ProfileService profileService){

        this.profileService = profileService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Profile>> getAllProfiles() {
        List<Profile> profile = profileService.getAllProfiles();
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/")
    public ResponseEntity<Profile> saveProfile(@RequestBody Profile profile) {
        Profile savedProfile=profileService.saveProfile(profile);
        return ResponseEntity.ok(savedProfile);
    }

    // Get Profile by ID
    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable Long id) {
        Optional<Profile> profileOpt = profileService.getProfileById(id);
        return profileOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update Profile by ID
    @PutMapping("/{id}")
    public ResponseEntity<Profile> updateProfile(@PathVariable Long id, @RequestBody Profile profile) {
        Optional<Profile> existingProfile = profileService.getProfileById(id);
        if (!existingProfile.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        profile.setProfileId(id);  // ensure the ID stays the same
        Profile updatedProfile = profileService.saveProfile(profile);
        return ResponseEntity.ok(updatedProfile);
    }

    // Delete Profile by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        profileService.deleteProfileById(id);
        return ResponseEntity.noContent().build();
    }

}
