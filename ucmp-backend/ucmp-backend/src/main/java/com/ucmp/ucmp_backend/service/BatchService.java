package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.Faculty;
import com.ucmp.ucmp_backend.model.Batch;

import com.ucmp.ucmp_backend.repository.BatchRepository;
import com.ucmp.ucmp_backend.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BatchService {
    private final BatchRepository batchRepository;
    private final FacultyRepository facultyRepository;

    public  BatchService(BatchRepository batchRepository, FacultyRepository facultyRepository) {
        this.batchRepository = batchRepository;
        this.facultyRepository = facultyRepository;
    }
    public Batch saveBatch(Batch batch){
        return batchRepository.save(batch);
    }

    public Optional<Batch> findById(Long id) {
        return batchRepository.findById(id);
    }
    public Optional<Batch> findByBatchName(String name){
        return batchRepository.findByBatchName(name);
    }
    public void deleteBatch(Long id) {
        batchRepository.deleteById(id);
    }

    public Batch assignFacultyToBatch(Long batchId, Long facultyId) {
        Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not Found"));
        Faculty faculty = facultyRepository.findById(facultyId).orElseThrow(() -> new RuntimeException("Faculty not Found"));

        batch.getFaculties().add(faculty);
        return batchRepository.save(batch);
    }
}
