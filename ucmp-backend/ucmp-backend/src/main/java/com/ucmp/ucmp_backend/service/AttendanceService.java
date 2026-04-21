package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public AttendanceSession startSession(Long facultyId, Long sectionId, Double latitude, Double longitude, Double radiusInMeters) {
        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new RuntimeException("Faculty not found"));
        // Inside AttendanceService.java
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        // Close any other active sessions for this faculty
        List<AttendanceSession> activeSessions = sessionRepository.findByFacultyIdAndIsActiveTrue(facultyId);
        for (AttendanceSession s : activeSessions) {
            s.setActive(false);
            s.setEndTime(LocalDateTime.now());
            sessionRepository.save(s);
        }

        AttendanceSession newSession = AttendanceSession.builder()
            .faculty(faculty)
            .section(section)
            .latitude(latitude)
            .longitude(longitude)
            .radiusInMeters(radiusInMeters != null ? radiusInMeters : 50.0)
            .secretSeed(UUID.randomUUID().toString())
            .startTime(LocalDateTime.now())
            .isActive(true)
            .build();

        return sessionRepository.save(newSession);
    }

    public String getCurrentCodeForSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findByIdAndIsActiveTrue(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not active or not found"));
        return generateCodeForTime(session.getSecretSeed(), System.currentTimeMillis());
    }

    @Transactional
    public void markAttendance(Long sessionId, Long studentId, String submittedCode, Double latitude, Double longitude) {
        AttendanceSession session = sessionRepository.findByIdAndIsActiveTrue(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not active or not found"));
            
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        if (recordRepository.existsByStudentIdAndSessionId(studentId, sessionId)) {
            throw new RuntimeException("Attendance already marked for this session");
        }

        // Validate Location (if provided by session)
        if (session.getLatitude() != null && session.getLongitude() != null) {
            if (latitude == null || longitude == null) {
                throw new RuntimeException("Location data is required for this session");
            }
            double distance = calculateHaversineDistance(session.getLatitude(), session.getLongitude(), latitude, longitude);
            if (distance > session.getRadiusInMeters()) {
                throw new RuntimeException("You are too far from the classroom (" + Math.round(distance) + " meters away). Max allowed: " + session.getRadiusInMeters() + "m");
            }
        }

        // Validate Code (Check current window and previous window to handle network delay)
        long currentMillis = System.currentTimeMillis();
        String currentCode = generateCodeForTime(session.getSecretSeed(), currentMillis);
        String previousCode = generateCodeForTime(session.getSecretSeed(), currentMillis - 15000);

        if (!submittedCode.equals(currentCode) && !submittedCode.equals(previousCode)) {
            throw new RuntimeException("Code is invalid or has expired.");
        }

        // Create Record
        AttendanceRecord record = AttendanceRecord.builder()
            .session(session)
            .student(student)
            .markedAt(LocalDateTime.now())
            .markedLatitude(latitude)
            .markedLongitude(longitude)
            .build();

        recordRepository.save(record);
    }

    @Transactional
    public void endSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setActive(false);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    // --- Utility Methods ---

    public static String generateCodeForTime(String seed, long timeMillis) {
        // 15-second windows
        long timeWindow = timeMillis / 15000; 
        String input = seed + timeWindow;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            int offset = hash.length - 4;
            // Get positive integer
            int binary = ((hash[offset] & 0x7f) << 24) |
                         ((hash[offset + 1] & 0xff) << 16) |
                         ((hash[offset + 2] & 0xff) << 8) |
                         (hash[offset + 3] & 0xff);
            int otp = binary % 1000000; // 6 digits
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Code generation failed", e);
        }
    }

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = R * c;
        return distanceKm * 1000; // Return in meters
    }
}
