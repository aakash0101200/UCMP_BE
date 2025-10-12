package com.ucmp.ucmp_backend.config;

import com.ucmp.ucmp_backend.model.Role;
import com.ucmp.ucmp_backend.model.RoleName;
import com.ucmp.ucmp_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner { //roleSeeder

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRoles();
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            Role studentRole = new Role(null, RoleName.STUDENT);
            Role facultyRole = new Role(null, RoleName.FACULTY);
            Role adminRole   = new Role(null, RoleName.ADMIN);

            roleRepository.saveAll(List.of(studentRole, facultyRole, adminRole));
        }
    }
}