package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Room;
import com.ucmp.ucmp_backend.model.RoomStatus;
import com.ucmp.ucmp_backend.model.RoomType;
import com.ucmp.ucmp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;

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
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        if (room.getStatus() == null) room.setStatus(RoomStatus.AVAILABLE);
        return ResponseEntity.ok(roomRepository.save(room));
    }

    /** PUT /api/rooms/{id}/status — Mark room as MAINTENANCE / AVAILABLE (Admin only) */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam RoomStatus status) {
        return roomRepository.findById(id).map(room -> {
            room.setStatus(status);
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/rooms/{id} — Full update (Admin only) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Room updated) {
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
    public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomRepository.deleteById(id);
        return ResponseEntity.ok("Room deleted");
    }
}
