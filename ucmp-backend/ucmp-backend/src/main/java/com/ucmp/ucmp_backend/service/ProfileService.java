package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class ProfileService {
    private final ProfileRepository profileRepository;

 @Autowired
    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }

    public List<Profile> getAllProfiles() {
     return profileRepository.findAll();
    }

    public Optional<Profile> getProfileById(Long profileId) {
        return profileRepository.findById(profileId);
    }

    public void deleteProfileById(Long id) {
        profileRepository.deleteById(id);
    }
}
