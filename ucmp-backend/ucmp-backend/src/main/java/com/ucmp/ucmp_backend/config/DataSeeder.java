package com.ucmp.ucmp_backend.config;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.RoleRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import com.ucmp.ucmp_backend.repository.BatchRepository;
import com.ucmp.ucmp_backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 1. Ensure the ADMIN role exists in the database
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.ADMIN);
                    return roleRepository.save(newRole);
                });

        // 2. Check if our default Super Admin already exists
        String adminCollegeId = "ADMIN_001";

        if (!userRepository.existsByCollegeId(adminCollegeId)) {
            // Create the User
            User adminUser = User.builder()
                    .collegeId(adminCollegeId)
                    .name("Super Admin")
                    .email("admin@ucmp.edu")
                    .password(passwordEncoder.encode("Admin@123")) // Default password
                    .roles(Collections.singleton(adminRole))
                    .build();

            userRepository.save(adminUser);

            // Create the Profile linking
            Profile adminProfile = new Profile();
            adminProfile.setUser_CollegeId(adminUser.getCollegeId());
            adminProfile.setName(adminUser.getName());
            adminProfile.setEmail(adminUser.getEmail());
            adminProfile.setUser(adminUser);

            profileRepository.save(adminProfile);

            System.out.println("✅ Super Admin account generated successfully.");
        }

        // 3. Create a default Batch and Section for testing
        Batch defaultBatch = batchRepository.findById(1L).orElseGet(() -> {
            Batch newBatch = new Batch();
            newBatch.setBatchName("B.Tech CS 2026");
            return batchRepository.save(newBatch);
        });

        if (sectionRepository.findById(1L).isEmpty()) {
            Section defaultSection = new Section();
            defaultSection.setSectionName("Section A");
            defaultSection.setBatch(defaultBatch);
            sectionRepository.save(defaultSection);
            System.out.println("✅ Default Batch and Section generated successfully.");
        }
    }
}