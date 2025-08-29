package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.dto.ProfileUpdateRequest;
import com.ucmp.ucmp_backend.model.Profile;
import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.ProfileRepository;
import com.ucmp.ucmp_backend.repository.StudentRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ProfileRepository profileRepository;

    public Profile  updateProfile(String collegeId, ProfileUpdateRequest request){
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("User not Found"));
        //update user basic info
         if(request.getName() == null){
             user.setName(request.getName());
             if(user.getProfile()!= null){
                 user.getProfile().setName(request.getName());
             }
         }
         if(request.getProfilePictureUrl() != null){
             user.getProfile().setProfilePictureUrl(request.getProfilePictureUrl());
         }

         //Role-based updates
        if(user.getRole() == Role.STUDENT) {
            Student student = user.getStudent();
            if(student == null) throw new RuntimeException("Student Profile not Found");
            if(request.getDepartment()!= null)
                student.setDepartment(request.getDepartment());
            if (request.getYear() != null)
                student.setYear(request.getYear());
            if (request.getRollNumber() != null)
                student.setRollNumber(request.getRollNumber());
        } else if (user.getRole() == Role.FACULTY) {
            Faculty faculty = user.getFaculty();
            if(faculty == null) throw new RuntimeException("Faculty not Found");
            if (request.getDepartment() != null)
                faculty.setDepartment(request.getDepartment());
            if (request.getDesignation()!= null)
                faculty.setDesignation(request.getDesignation());
            if(request.getOfficeLocation()!= null)
                faculty.setOfficeLocation(request.getOfficeLocation());
            if (request.getOfficeHours() != null)
                faculty.setOfficeHours(request.getOfficeHours());
        }

        //Save changes
        userRepository.save(user);
        return user.getProfile();
    }

}
