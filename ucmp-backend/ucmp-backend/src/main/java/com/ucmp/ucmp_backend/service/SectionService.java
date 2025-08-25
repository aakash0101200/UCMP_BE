package com.ucmp.ucmp_backend.service;


import com.ucmp.ucmp_backend.dto.StudentProfileDTO;
import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Section;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import com.ucmp.ucmp_backend.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {
    private final SectionRepository sectionRepository;
    private final FacultyRepository facultyRepository;

    public SectionService(SectionRepository sectionRepository, FacultyRepository facultyRepository) {
        this.sectionRepository = sectionRepository;
        this.facultyRepository = facultyRepository;
    }

    public Section saveSection(Section section){
        return sectionRepository.save(section);
    }
    public  Section updateSection(Section section){
        return sectionRepository.save(section);
    }

    public List<Section> getAllSections(){
        return sectionRepository.findAll();
    }

    public Optional<Section> getSectionById(Long id){
        return sectionRepository.findById(id);
    }

    public void deleteSection(Long id){
        sectionRepository.deleteById(id);
    }

    //assign Faculty to section

    public Section assignFacultyToSection(Long sectionId, Long facultyId){
        Section section = sectionRepository.findById(sectionId).orElseThrow(() -> new RuntimeException("Section not found"));
        Faculty faculty = facultyRepository.findById(facultyId).orElseThrow(() -> new RuntimeException("Faculty not found"));

        section.getFaculties().add(faculty);
        return sectionRepository.save(section);
    }

    public Section removeFacultyFromSection(Long sectionId, Long facultyId){
        Section section = sectionRepository.findById(sectionId).orElseThrow(() -> new RuntimeException("Section not found"));
        Faculty faculty = facultyRepository.findById(facultyId).orElseThrow(() -> new RuntimeException("Faculty not found"));

        section.getFaculties().remove(faculty);
        return sectionRepository.save(section);
    }

}