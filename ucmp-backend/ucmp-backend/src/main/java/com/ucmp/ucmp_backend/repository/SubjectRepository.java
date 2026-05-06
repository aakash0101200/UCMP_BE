package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Subject;
import com.ucmp.ucmp_backend.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    List<Subject> findByDepartment(String department);

    // Subjects that require a specific room type (labs, etc.)
    List<Subject> findByRequiredRoomType(RoomType roomType);

    boolean existsByCode(String code);
}
