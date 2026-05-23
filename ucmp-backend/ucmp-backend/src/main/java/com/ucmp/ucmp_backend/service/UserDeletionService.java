package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.model.*;
import com.ucmp.ucmp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ClassCancellationRepository classCancellationRepository;
    private final TimetableOverrideRepository timetableOverrideRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    @Transactional
    public void deleteUserCascaded(String collegeId) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with college ID: " + collegeId));

        Student student = user.getStudent();
        if (student != null) {
            // Delete associated attendance records first
            attendanceRecordRepository.deleteByStudentId(student.getId());
        }

        Faculty faculty = user.getFaculty();
        if (faculty != null) {
            Long facultyId = faculty.getId();

            // 1. Nullify references in AttendanceRecord (where faculty manually marked attendance)
            attendanceRecordRepository.nullifyFacultyReferences(facultyId);

            // 2. Fetch all AttendanceSessions conducted by this faculty
            List<AttendanceSession> sessions = attendanceSessionRepository.findByFacultyId(facultyId);
            for (AttendanceSession session : sessions) {
                // Delete AttendanceRecords for this session
                attendanceRecordRepository.deleteByAttendanceSessionId(session.getId());
            }
            // Delete the sessions themselves
            attendanceSessionRepository.deleteAll(sessions);

            // 3. Nullify scheduledFaculty references in other sessions
            attendanceSessionRepository.nullifyScheduledFacultyReferences(facultyId);

            // 4. Nullify original/new faculty links in TimetableOverrides
            timetableOverrideRepository.nullifyOriginalFacultyReferences(facultyId);
            timetableOverrideRepository.nullifyNewFacultyReferences(facultyId);

            // 5. Nullify reviewed faculty links in ClassCancellations
            classCancellationRepository.nullifyReviewedByReferences(facultyId);

            // 6. Delete class cancellations initiated by this faculty
            classCancellationRepository.deleteByCancelledById(facultyId);

            // 7. Fetch all TimetableEntry records taught by this faculty
            List<TimetableEntry> entries = timetableEntryRepository.findByFacultyId(facultyId);
            for (TimetableEntry entry : entries) {
                // Nullify timetableEntry link in TimetableOverrides
                timetableOverrideRepository.nullifyTimetableEntryReferences(entry.getId());
                // Delete cancellations referencing this timetable entry
                classCancellationRepository.deleteByTimetableEntryId(entry.getId());
            }
            // Delete the timetable entries themselves
            timetableEntryRepository.deleteAll(entries);

            // 8. Delete subject assignments assigned to this faculty
            subjectAssignmentRepository.deleteByFacultyId(facultyId);
        }

        // Finally, delete the user which cascades to Student / Faculty / Profile via JPA CascadeType.ALL
        userRepository.deleteByCollegeId(collegeId);
    }
}
