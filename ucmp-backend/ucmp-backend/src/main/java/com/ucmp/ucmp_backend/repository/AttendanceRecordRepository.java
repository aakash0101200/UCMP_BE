package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    boolean existsByStudentIdAndAttendanceSessionId(Long studentId, Long sessionId);
    List<AttendanceRecord> findByAttendanceSessionId(Long sessionId);
}
