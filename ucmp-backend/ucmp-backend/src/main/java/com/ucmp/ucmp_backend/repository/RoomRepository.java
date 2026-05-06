package com.ucmp.ucmp_backend.repository;

import com.ucmp.ucmp_backend.model.Room;
import com.ucmp.ucmp_backend.model.RoomStatus;
import com.ucmp.ucmp_backend.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Find all rooms of a specific type (e.g. all CS_LABs)
    List<Room> findByType(RoomType type);

    // Find all available rooms
    List<Room> findByStatus(RoomStatus status);

    // Find available rooms of a specific type (used by auto-generator)
    List<Room> findByTypeAndStatus(RoomType type, RoomStatus status);

    // Find all rooms in a building
    List<Room> findByBuilding(String building);

    // Find rooms in a building by status (for bulk maintenance marking)
    List<Room> findByBuildingAndStatus(String building, RoomStatus status);

    // Find rooms with enough capacity for a section
    List<Room> findByCapacityGreaterThanEqual(int minCapacity);
}
