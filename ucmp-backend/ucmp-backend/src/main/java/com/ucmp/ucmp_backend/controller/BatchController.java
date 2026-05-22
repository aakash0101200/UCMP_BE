package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.dto.BatchDTO;
import com.ucmp.ucmp_backend.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchRepository batchRepository;

    @GetMapping
    public ResponseEntity<List<BatchDTO>> getAllBatches() {
        List<BatchDTO> batches = batchRepository.findAll().stream()
                .map(b -> new BatchDTO(b.getId(), b.getBatchName()))
                .toList();
        return ResponseEntity.ok(batches);
    }
}
