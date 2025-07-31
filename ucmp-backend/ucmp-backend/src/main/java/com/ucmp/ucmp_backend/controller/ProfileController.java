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
    public ResponseEntity<Profile> addProfile(@RequestBody Profile profile) {
        Profile savedProfile=profileService.createProfile(profile);
        return ResponseEntity.ok(savedProfile);
    }

}
