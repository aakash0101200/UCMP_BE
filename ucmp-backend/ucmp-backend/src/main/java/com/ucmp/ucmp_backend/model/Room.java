package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_name_building", columnNames = {"name", "building"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;           // e.g. "Room 301", "CS Lab 2"

    @NotBlank
    @Column(nullable = false)
    private String building;       // e.g. "Main Block", "IT Block"

    @Min(1)
    @Column(nullable = false)
    private int capacity;          // e.g. 60

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;         // LECTURE_HALL, CS_LAB, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;
}
