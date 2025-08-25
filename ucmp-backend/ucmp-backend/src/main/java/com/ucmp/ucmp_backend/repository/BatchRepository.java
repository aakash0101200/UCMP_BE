package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Batch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByBatchName(String batchName);

}
