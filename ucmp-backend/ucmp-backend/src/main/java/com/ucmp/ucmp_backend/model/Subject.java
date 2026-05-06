package com.ucmp.ucmp_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(
    name = "subjects",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_code", columnNames = {"code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;           // e.g. "Data Structures"

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;           // e.g. "CS301"

    @Min(1)
    @Column(nullable = false)
    private int credits;           // e.g. 4

    /**
     * Total lectures per week for this subject.
     * e.g. 4 for a 4-credit theory subject, 6 for theory+lab combined.
     */
    @Min(1)
    @Column(nullable = false)
    private int weeklyHours;

    /**
     * Duration of a single slot in hours.
     * 1 = lecture (1 hour slot)
     * 2 = lab (2 consecutive hours, no break allowed in between)
     */
    @Column(nullable = false)
    @Builder.Default
    private int slotDuration = 1;

    /**
     * The type of room this subject requires.
     * e.g. CS_LAB for programming practicals, ANY for regular lectures.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomType requiredRoomType = RoomType.ANY;

    private String department;     // e.g. "Computer Science"
}
