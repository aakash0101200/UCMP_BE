package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.Student;
import com.ucmp.ucmp_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> getProfileByProfileId(Long profileId);
    Optional<Profile> getProfilesByCollegeId(String collegeId);



}




