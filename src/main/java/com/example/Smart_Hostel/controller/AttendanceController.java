package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.Attendance;
import com.example.Smart_Hostel.entity.Student;
import com.example.Smart_Hostel.repository.AttendanceRepository;
import com.example.Smart_Hostel.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public List<Attendance> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long studentId) {

        if (date != null && studentId != null) {
            return attendanceRepository.findByStudentIdAndDate(studentId, date)
                    .map(List::of).orElse(List.of());
        }
        if (date != null) {
            return attendanceRepository.findByDate(date);
        }
        if (studentId != null) {
            return attendanceRepository.findByStudentId(studentId);
        }
        return attendanceRepository.findByDate(LocalDate.now());
    }

    @PostMapping
    public ResponseEntity<?> markAttendance(@RequestBody List<AttendanceRequest> requests) {
        List<Attendance> saved = requests.stream().map(req -> {
            Student student = studentRepository.findById(req.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + req.getStudentId()));
            LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();

            Attendance attendance = attendanceRepository
                    .findByStudentIdAndDate(req.getStudentId(), date)
                    .orElse(Attendance.builder().student(student).date(date).build());

            attendance.setStatus(req.getStatus());
            attendance.setRemarks(req.getRemarks());
            return attendanceRepository.save(attendance);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/stats")
    public Map<String, Object> getTodayStats() {
        LocalDate today = LocalDate.now();
        long present = attendanceRepository.countByDateAndStatus(today, Attendance.AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByDateAndStatus(today, Attendance.AttendanceStatus.ABSENT);
        long leave = attendanceRepository.countByDateAndStatus(today, Attendance.AttendanceStatus.LEAVE);
        long total = studentRepository.count();
        return Map.of(
                "date", today.toString(),
                "present", present,
                "absent", absent,
                "leave", leave,
                "totalStudents", total,
                "attendancePercentage", total > 0 ? Math.round((present * 100.0) / total) : 0
        );
    }

    @GetMapping("/student/{studentId}/percentage")
    public Map<String, Object> getStudentAttendancePercentage(@PathVariable Long studentId) {
        long totalDays = attendanceRepository.countTotalByStudentId(studentId);
        long presentDays = attendanceRepository.countPresentByStudentId(studentId);
        double percentage = totalDays > 0 ? (presentDays * 100.0) / totalDays : 0;
        return Map.of(
                "studentId", studentId,
                "totalDays", totalDays,
                "presentDays", presentDays,
                "percentage", Math.round(percentage)
        );
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    @lombok.Data
    public static class AttendanceRequest {
        private Long studentId;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Attendance.AttendanceStatus status;
        private String remarks;
    }
}

