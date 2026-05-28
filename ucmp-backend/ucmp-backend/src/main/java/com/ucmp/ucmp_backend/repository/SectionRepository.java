package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findBySectionNameIgnoreCaseAndBatchIdAndYear(String sectionName, Long batchId, Integer year);
}
