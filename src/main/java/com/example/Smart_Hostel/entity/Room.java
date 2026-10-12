package com.example.Smart_Hostel.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String roomNumber;

    @NotNull
    @Min(1)
    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer occupiedBeds = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoomType type = RoomType.SINGLE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column
    private String floor;

    @Column
    private String description;

    public enum RoomType { SINGLE, DOUBLE, TRIPLE, QUAD }
    public enum RoomStatus { AVAILABLE, FULL, MAINTENANCE }

    public int getAvailableBeds() {
        return Math.max(0, capacity - occupiedBeds);
    }
}
