package com.ucmp.ucmp_backend.config;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;

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

        // 2. Seed default Super Admin
        String adminCollegeId = "ADMIN_001";
        if (!userRepository.existsByCollegeId(adminCollegeId)) {
            User adminUser = User.builder()
                    .collegeId(adminCollegeId)
                    .name("Super Admin")
                    .email("admin@ucmp.edu")
                    .password(passwordEncoder.encode("Admin@123"))
                    .roles(Collections.singleton(adminRole))
                    .build();
            userRepository.save(adminUser);

            Profile adminProfile = new Profile();
            adminProfile.setUser_CollegeId(adminUser.getCollegeId());
            adminProfile.setName(adminUser.getName());
            adminProfile.setEmail(adminUser.getEmail());
            adminProfile.setUser(adminUser);
            profileRepository.save(adminProfile);

            System.out.println("✅ Super Admin account generated successfully.");
        }

        // 3. Seed default Batch and Section
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

        // 4. Seed sample Rooms (only if none exist)
        if (roomRepository.count() == 0) {
            List<Room> sampleRooms = List.of(
                Room.builder().name("Room 101").building("Main Block").capacity(60).type(RoomType.LECTURE_HALL).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("Room 102").building("Main Block").capacity(60).type(RoomType.LECTURE_HALL).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("Room 201").building("Main Block").capacity(80).type(RoomType.LECTURE_HALL).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("Room 202").building("Main Block").capacity(80).type(RoomType.LECTURE_HALL).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("Seminar Hall A").building("Main Block").capacity(150).type(RoomType.SEMINAR_HALL).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("CS Lab 1").building("IT Block").capacity(40).type(RoomType.CS_LAB).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("CS Lab 2").building("IT Block").capacity(40).type(RoomType.CS_LAB).status(RoomStatus.AVAILABLE).build(),
                Room.builder().name("Electronics Lab").building("Science Block").capacity(30).type(RoomType.ELECTRONICS_LAB).status(RoomStatus.AVAILABLE).build()
            );
            roomRepository.saveAll(sampleRooms);
            System.out.println("✅ Sample Rooms seeded (" + sampleRooms.size() + " rooms).");
        }

        // 5. Seed sample Subjects (only if none exist)
        if (subjectRepository.count() == 0) {
            List<Subject> sampleSubjects = List.of(
                Subject.builder().name("Data Structures").code("CS301").credits(4).weeklyHours(4).slotDuration(1).requiredRoomType(RoomType.ANY).department("Computer Science").build(),
                Subject.builder().name("Algorithms").code("CS302").credits(4).weeklyHours(4).slotDuration(1).requiredRoomType(RoomType.ANY).department("Computer Science").build(),
                Subject.builder().name("Operating Systems").code("CS303").credits(4).weeklyHours(4).slotDuration(1).requiredRoomType(RoomType.ANY).department("Computer Science").build(),
                Subject.builder().name("Database Systems").code("CS304").credits(4).weeklyHours(4).slotDuration(1).requiredRoomType(RoomType.ANY).department("Computer Science").build(),
                Subject.builder().name("Programming Lab").code("CS301L").credits(2).weeklyHours(2).slotDuration(2).requiredRoomType(RoomType.CS_LAB).department("Computer Science").build(),
                Subject.builder().name("DBMS Lab").code("CS304L").credits(2).weeklyHours(2).slotDuration(2).requiredRoomType(RoomType.CS_LAB).department("Computer Science").build(),
                Subject.builder().name("Mathematics I").code("MA101").credits(4).weeklyHours(4).slotDuration(1).requiredRoomType(RoomType.ANY).department("Mathematics").build(),
                Subject.builder().name("Engineering Physics").code("PH101").credits(3).weeklyHours(3).slotDuration(1).requiredRoomType(RoomType.ANY).department("Physics").build()
            );
            subjectRepository.saveAll(sampleSubjects);
            System.out.println("✅ Sample Subjects seeded (" + sampleSubjects.size() + " subjects).");
        }
    }
}