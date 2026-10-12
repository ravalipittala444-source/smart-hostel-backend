package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.Room;
import com.example.Smart_Hostel.entity.Student;
import com.example.Smart_Hostel.repository.RoomRepository;
import com.example.Smart_Hostel.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/available")
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(Room.RoomStatus.AVAILABLE);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@Valid @RequestBody Room room) {
        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room number already exists"));
        }
        room.setOccupiedBeds(0);
        room.setStatus(Room.RoomStatus.AVAILABLE);
        return ResponseEntity.ok(roomRepository.save(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @Valid @RequestBody Room updated) {
        return roomRepository.findById(id).map(room -> {
            room.setRoomNumber(updated.getRoomNumber());
            room.setCapacity(updated.getCapacity());
            room.setType(updated.getType());
            room.setFloor(updated.getFloor());
            room.setDescription(updated.getDescription());
            // Recalculate status
            if (updated.getStatus() != null) {
                room.setStatus(updated.getStatus());
            } else {
                room.setStatus(room.getOccupiedBeds() >= room.getCapacity()
                        ? Room.RoomStatus.FULL : Room.RoomStatus.AVAILABLE);
            }
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Allocate a student to a room */
    @PostMapping("/{id}/allocate")
    public ResponseEntity<?> allocateStudent(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        return roomRepository.findById(id).map(room -> {
            if (room.getOccupiedBeds() >= room.getCapacity()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Room is at full capacity"));
            }
            return studentRepository.findById(studentId).map(student -> {
                // Remove from old room
                if (student.getRoom() != null) {
                    Room oldRoom = student.getRoom();
                    oldRoom.setOccupiedBeds(Math.max(0, oldRoom.getOccupiedBeds() - 1));
                    if (oldRoom.getOccupiedBeds() < oldRoom.getCapacity()) {
                        oldRoom.setStatus(Room.RoomStatus.AVAILABLE);
                    }
                    roomRepository.save(oldRoom);
                }
                student.setRoom(room);
                studentRepository.save(student);
                room.setOccupiedBeds(room.getOccupiedBeds() + 1);
                if (room.getOccupiedBeds() >= room.getCapacity()) {
                    room.setStatus(Room.RoomStatus.FULL);
                }
                roomRepository.save(room);
                return ResponseEntity.ok(Map.of("message", "Student allocated successfully"));
            }).orElse(ResponseEntity.badRequest().body(Map.of("error", "Student not found")));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Vacate a student from a room */
    @PostMapping("/{id}/vacate")
    public ResponseEntity<?> vacateStudent(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        return roomRepository.findById(id).map(room -> {
            return studentRepository.findById(studentId).map(student -> {
                student.setRoom(null);
                studentRepository.save(student);
                room.setOccupiedBeds(Math.max(0, room.getOccupiedBeds() - 1));
                if (room.getStatus() == Room.RoomStatus.FULL) {
                    room.setStatus(Room.RoomStatus.AVAILABLE);
                }
                roomRepository.save(room);
                return ResponseEntity.ok(Map.of("message", "Student vacated successfully"));
            }).orElse(ResponseEntity.badRequest().body(Map.of("error", "Student not found")));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> getRoomStats() {
        List<Room> rooms = roomRepository.findAll();
        long totalRooms = rooms.size();
        long availableRooms = rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.AVAILABLE).count();
        long fullRooms = rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.FULL).count();
        int totalBeds = rooms.stream().mapToInt(Room::getCapacity).sum();
        int occupiedBeds = rooms.stream().mapToInt(Room::getOccupiedBeds).sum();
        return Map.of(
                "totalRooms", totalRooms,
                "availableRooms", availableRooms,
                "fullRooms", fullRooms,
                "totalBeds", totalBeds,
                "occupiedBeds", occupiedBeds,
                "availableBeds", totalBeds - occupiedBeds
        );
    }
}

