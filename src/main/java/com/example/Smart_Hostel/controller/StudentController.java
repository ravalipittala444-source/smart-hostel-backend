package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.Student;
import com.example.Smart_Hostel.entity.Room;
import com.example.Smart_Hostel.repository.StudentRepository;
import com.example.Smart_Hostel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;

    @GetMapping
    public List<Student> getAllStudents(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return studentRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        }
        return studentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStudent(@Valid @RequestBody StudentRequest req) {
        if (studentRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        Student student = Student.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .gender(req.getGender())
                .course(req.getCourse())
                .yearOfStudy(req.getYearOfStudy())
                .status(Student.StudentStatus.ACTIVE)
                .build();

        if (req.getRoomId() != null) {
            roomRepository.findById(req.getRoomId()).ifPresent(room -> {
                student.setRoom(room);
                room.setOccupiedBeds(room.getOccupiedBeds() + 1);
                if (room.getOccupiedBeds() >= room.getCapacity()) {
                    room.setStatus(Room.RoomStatus.FULL);
                }
                roomRepository.save(room);
            });
        }
        return ResponseEntity.ok(studentRepository.save(student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest req) {
        return studentRepository.findById(id).map(student -> {
            student.setFullName(req.getFullName());
            student.setPhone(req.getPhone());
            student.setGender(req.getGender());
            student.setCourse(req.getCourse());
            student.setYearOfStudy(req.getYearOfStudy());

            // Handle room change
            Long oldRoomId = student.getRoom() != null ? student.getRoom().getId() : null;
            Long newRoomId = req.getRoomId();

            if (!java.util.Objects.equals(oldRoomId, newRoomId)) {
                if (oldRoomId != null) {
                    roomRepository.findById(oldRoomId).ifPresent(old -> {
                        old.setOccupiedBeds(Math.max(0, old.getOccupiedBeds() - 1));
                        old.setStatus(Room.RoomStatus.AVAILABLE);
                        roomRepository.save(old);
                    });
                }
                if (newRoomId != null) {
                    roomRepository.findById(newRoomId).ifPresent(newRoom -> {
                        student.setRoom(newRoom);
                        newRoom.setOccupiedBeds(newRoom.getOccupiedBeds() + 1);
                        if (newRoom.getOccupiedBeds() >= newRoom.getCapacity()) {
                            newRoom.setStatus(Room.RoomStatus.FULL);
                        }
                        roomRepository.save(newRoom);
                    });
                } else {
                    student.setRoom(null);
                }
            }
            return ResponseEntity.ok(studentRepository.save(student));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        return studentRepository.findById(id).map(student -> {
            if (student.getRoom() != null) {
                Room room = student.getRoom();
                room.setOccupiedBeds(Math.max(0, room.getOccupiedBeds() - 1));
                room.setStatus(Room.RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
            studentRepository.delete(student);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public Map<String, Long> getStudentCount() {
        return Map.of(
                "total", studentRepository.count(),
                "active", studentRepository.countByStatus(Student.StudentStatus.ACTIVE)
        );
    }

    // ── Inner DTO ──────────────────────────────────────────────────────────────

    @lombok.Data
    public static class StudentRequest {
        @jakarta.validation.constraints.NotBlank
        private String fullName;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String email;
        private String phone;
        private Student.Gender gender;
        private String course;
        private Integer yearOfStudy;
        private Long roomId;
    }
}

