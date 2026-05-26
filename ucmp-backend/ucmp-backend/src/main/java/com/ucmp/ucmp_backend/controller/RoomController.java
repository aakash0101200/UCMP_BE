package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Room;
import com.ucmp.ucmp_backend.model.RoomStatus;
import com.ucmp.ucmp_backend.model.RoomType;
import com.ucmp.ucmp_backend.model.User;
import com.ucmp.ucmp_backend.repository.RoomRepository;
import com.ucmp.ucmp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    private void enforceSuperAdmin(Authentication authentication) {
        String collegeId = authentication.getName();
        User adminUser = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new RuntimeException("Logged in user is not found"));
        boolean isSuper = "ADMIN_001".equals(collegeId) || adminUser.getDepartment() == null || "Administration".equalsIgnoreCase(adminUser.getDepartment());
        if (!isSuper) {
            throw new RuntimeException("Access Denied: Only Super Admin can manage physical venues/rooms.");
        }
    }

    /** GET /api/rooms — List all rooms */
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    /** GET /api/rooms/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoom(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/rooms/available — All rooms that can be booked */
    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        return ResponseEntity.ok(roomRepository.findByStatus(RoomStatus.AVAILABLE));
    }

    /** GET /api/rooms/type/{type} — e.g. /api/rooms/type/CS_LAB */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Room>> getRoomsByType(@PathVariable RoomType type) {
        return ResponseEntity.ok(roomRepository.findByType(type));
    }

    /** POST /api/rooms — Create a room (Admin only) */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Room> createRoom(Authentication authentication, @RequestBody Room room) {
        enforceSuperAdmin(authentication);
        if (room.getStatus() == null) room.setStatus(RoomStatus.AVAILABLE);
        return ResponseEntity.ok(roomRepository.save(room));
    }

    /** PUT /api/rooms/{id}/status — Mark room as MAINTENANCE / AVAILABLE (Admin only) */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateRoomStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam RoomStatus status) {
        enforceSuperAdmin(authentication);
        return roomRepository.findById(id).map(room -> {
            room.setStatus(status);
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/rooms/{id} — Full update (Admin only) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateRoom(Authentication authentication, @PathVariable Long id, @RequestBody Room updated) {
        enforceSuperAdmin(authentication);
        return roomRepository.findById(id).map(room -> {
            room.setName(updated.getName());
            room.setBuilding(updated.getBuilding());
            room.setCapacity(updated.getCapacity());
            room.setType(updated.getType());
            room.setStatus(updated.getStatus());
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/rooms/{id} (Admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteRoom(Authentication authentication, @PathVariable Long id) {
        enforceSuperAdmin(authentication);
        if (!roomRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomRepository.deleteById(id);
        return ResponseEntity.ok("Room deleted");
    }
}
