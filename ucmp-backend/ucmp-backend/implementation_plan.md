# Master Timetable Engine & Smart Faculty Replacement System

This implementation plan details the addition of a dynamic scheduling layer to the timetable system, transitioning from static weekly templates to date-specific schedule modifications, cancellations, substitutions, and a live availability checker.

---

## User Review Required

> [!IMPORTANT]
> **Substitutions and Overrides Database Schema**: We will introduce a new database table `timetable_substitutions` that records date-specific faculty overrides. This preserves the static timetable templates (`timetable_entries`) and stores daily schedule modifications as exceptions.
> 
> **Attendance Syncing**: When a substitution is active, the system resolves the schedule for that date to the substituted faculty. When that faculty starts an attendance session, it is correctly linked to them, and counts towards students' attendance for that subject automatically.
> 
> **Live Availability Calculation**: True availability for any faculty member at a given date/time slot accounts for:
> - Recurring scheduled slots.
> - Date-specific cancellations (which free them up).
> - Date-specific substitutions (which make them busy if covering, or free them up if covered).

---

## Open Questions

> [!NOTE]
> None. The requirements for the substitution flow ("Call Teacher → Confirm → Click Assign") and the live status indicators are fully clear.

---

## Proposed Changes

### Backend (Spring Boot)

#### [NEW] [TimetableSubstitution.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/model/TimetableSubstitution.java)
- Define the substitution entity to capture date-specific teacher overrides:
  - `id` (Long, auto-generated)
  - `timetableEntry` (TimetableEntry, many-to-one)
  - `substitutionDate` (LocalDate)
  - `originalFaculty` (Faculty, many-to-one)
  - `substitutedFaculty` (Faculty, many-to-one)
  - `alternativeSubject` (Subject, many-to-one, nullable)
  - `alternativeRoom` (Room, many-to-one, nullable)
  - `reason` (String)
  - `status` (String, default "ASSIGNED")
  - `createdAt` (LocalDateTime)

#### [NEW] [TimetableSubstitutionRepository.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/repository/TimetableSubstitutionRepository.java)
- Query methods to fetch substitutions:
  - `findBySubstitutionDate(LocalDate date)`
  - `findByTimetableEntryIdAndSubstitutionDate(Long entryId, LocalDate date)`
  - `findBySubstitutedFacultyIdAndSubstitutionDate(Long facultyId, LocalDate date)`
  - `findByOriginalFacultyIdAndSubstitutionDate(Long facultyId, LocalDate date)`

#### [NEW] [FacultyAvailabilityResponse.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/dto/FacultyAvailabilityResponse.java)
- DTO containing faculty member information, live status (FREE or BUSY), current location/slot description if BUSY, and subject expertise matching.

#### [MODIFY] [TimetableEntryResponseDTO.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/dto/TimetableEntryResponseDTO.java)
- Add date-resolved fields:
  - `private LocalDate resolvedDate;`
  - `private boolean isCancelled;`
  - `private String cancellationReason;`
  - `private boolean isSubstituted;`
  - `private String substitutionReason;`
  - `private Long originalFacultyId;`
  - `private String originalFacultyName;`

#### [MODIFY] [TimetableService.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/service/TimetableService.java)
- Implement substitution APIs:
  - `assignSubstitution(...)`: save a substitution, create/broadcast notification events.
  - `removeSubstitution(Long entryId, LocalDate date)`: delete a substitution.
- Implement date-resolved schedule loaders:
  - `getResolvedScheduleForSection(Long sectionId, LocalDate date, String term)`
  - `getResolvedScheduleForFaculty(Long facultyId, LocalDate date, String term)`
- Implement live availability engine:
  - `getFacultyAvailability(LocalDate date, LocalTime startTime, LocalTime endTime, Long subjectId)`:
    - Iterate all faculties.
    - Check if faculty has a recurring entry at this day/time.
    - Resolve cancellations/substitutions for that slot.
    - Check if faculty is covering another class on this date/time.
    - Map expertise rank (direct assignment vs department match).

#### [MODIFY] [TimetableController.java](file:///D:/ucmp_be/ucmp-backend/ucmp-backend/src/main/java/com/ucmp/ucmp_backend/controller/TimetableController.java)
- Expose the following endpoints:
  - `GET /api/timetable/section/{sectionId}/date/{date}`
  - `GET /api/timetable/faculty/{facultyId}/date/{date}`
  - `GET /api/timetable/availability` (params: `date`, `startTime`, `endTime`, `subjectId`)
  - `POST /api/timetable/substitution`
  - `DELETE /api/timetable/substitution/entry/{entryId}/date/{date}`

---

### Frontend (React / Vite)

#### [MODIFY] [timetable.js](file:///c:/Users/aakas/frontend/src/Services/timetable.js)
- Add API helper functions:
  - `getSectionResolvedSchedule(sectionId, date, term)`
  - `getFacultyResolvedSchedule(facultyId, date, term)`
  - `getFacultyAvailability(date, startTime, endTime, subjectId)`
  - `createSubstitution(payload)`
  - `deleteSubstitution(entryId, date)`

#### [MODIFY] [WeeklyScheduleGrid.jsx](file:///c:/Users/aakas/frontend/src/components/Schedule/WeeklyScheduleGrid.jsx)
- Introduce a Date Picker and "Resolved vs Template" toggle.
- In resolved mode, fetch schedule using the new `date` endpoint and display detailed cancellation/substitution status badges.
- Enable substituted faculty to start attendance sessions directly from their daily schedule.

#### [MODIFY] [AdminTimetablePage.jsx](file:///c:/Users/aakas/frontend/src/pages/admin/AdminTimetablePage.jsx)
- Transform into a two-tab dashboard:
  - **Timetable Template Builder** (weekly template grid).
  - **Smart Faculty Replacement & Live Availability** (dynamic daily substitution panel).
- Smart Faculty Replacement dashboard components:
  - Search by absent teacher schedule & search by section daily overrides.
  - "Assign Substitution" button which opens a list of free faculty generated by the availability engine.
  - Action cards to instantly confirm replacements.

---

## Verification Plan

### Automated Tests
- Run maven build to confirm compilation.
- Verify status codes and responses of new availability check and substitution endpoints.

### Manual Verification
1. Access **Admin Console → Timetable → Smart Faculty Replacement**.
2. Mark a teacher as absent. View their classes for today.
3. Click **Assign Substitution** for a class. Notice the list of free teachers sorted by subject expertise.
4. Select a teacher and click **Assign**.
5. Log in as the student. Check **My Schedule** for today. The class should now show as "Substituted" with the new teacher's name.
6. Log in as the replacement teacher. Check **My Schedule** for today. The class should appear in their schedule. Start the attendance session and verify it is logged under their ID.
