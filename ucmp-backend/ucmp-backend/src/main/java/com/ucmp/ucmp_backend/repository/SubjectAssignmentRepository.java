package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.SubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment, Long> {

    // Get all assignments for a term — main input to the auto-generator
    List<SubjectAssignment> findByAcademicTerm(String academicTerm);

    // Get all assignments for a specific section in a term
    List<SubjectAssignment> findBySectionIdAndAcademicTerm(Long sectionId, String academicTerm);

    // Get all assignments for a specific faculty in a term
    List<SubjectAssignment> findByFacultyIdAndAcademicTerm(Long facultyId, String academicTerm);

    // Check if a faculty is already assigned a subject to a section in a term
    boolean existsByFacultyIdAndSubjectIdAndSectionIdAndAcademicTerm(
        Long facultyId, Long subjectId, Long sectionId, String academicTerm
    );
}
