package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Subject;
import com.ucmp.ucmp_backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;

    /** GET /api/subjects — All subjects */
    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(subjectRepository.findAll());
    }

    /** GET /api/subjects/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubject(@PathVariable Long id) {
        return subjectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/subjects/department/{dept} — e.g. /api/subjects/department/Computer Science */
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Subject>> getByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(subjectRepository.findByDepartment(department));
    }

    /** POST /api/subjects — Create subject (Admin only) */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createSubject(@RequestBody Subject subject) {
        if (subjectRepository.existsByCode(subject.getCode())) {
            return ResponseEntity.badRequest()
                    .body("Subject with code '" + subject.getCode() + "' already exists");
        }
        return ResponseEntity.ok(subjectRepository.save(subject));
    }

    /** PUT /api/subjects/{id} — Update subject (Admin only) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateSubject(@PathVariable Long id, @RequestBody Subject updated) {
        return subjectRepository.findById(id).map(subject -> {
            subject.setName(updated.getName());
            subject.setCode(updated.getCode());
            subject.setCredits(updated.getCredits());
            subject.setWeeklyHours(updated.getWeeklyHours());
            subject.setSlotDuration(updated.getSlotDuration());
            subject.setRequiredRoomType(updated.getRequiredRoomType());
            subject.setDepartment(updated.getDepartment());
            return ResponseEntity.ok(subjectRepository.save(subject));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/subjects/{id} (Admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteSubject(@PathVariable Long id) {
        if (!subjectRepository.existsById(id)) return ResponseEntity.notFound().build();
        subjectRepository.deleteById(id);
        return ResponseEntity.ok("Subject deleted");
    }
}
