package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.*;
import com.example.Smart_Hostel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final AttendanceRepository attendanceRepository;
    private final ComplaintRepository complaintRepository;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        long totalStudents = studentRepository.count();
        List<Room> rooms = roomRepository.findAll();
        long totalRooms = rooms.size();
        int totalBeds = rooms.stream().mapToInt(Room::getCapacity).sum();
        int occupiedBeds = rooms.stream().mapToInt(Room::getOccupiedBeds).sum();
        long availableBeds = totalBeds - occupiedBeds;

        LocalDate today = LocalDate.now();
        long todayPresent = attendanceRepository.countByDateAndStatus(today, Attendance.AttendanceStatus.PRESENT);
        long pendingComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalRooms", totalRooms);
        stats.put("availableBeds", availableBeds);
        stats.put("occupiedBeds", occupiedBeds);
        stats.put("todayAttendance", todayPresent);
        stats.put("pendingComplaints", pendingComplaints);
        return stats;
    }

    @GetMapping("/attendance-trend")
    public List<Map<String, Object>> getAttendanceTrend() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long present = attendanceRepository.countByDateAndStatus(date, Attendance.AttendanceStatus.PRESENT);
            long absent = attendanceRepository.countByDateAndStatus(date, Attendance.AttendanceStatus.ABSENT);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", date.toString());
            entry.put("day", date.getDayOfWeek().name().substring(0, 3));
            entry.put("present", present);
            entry.put("absent", absent);
            result.add(entry);
        }
        return result;
    }

    @GetMapping("/room-occupancy")
    public List<Map<String, Object>> getRoomOccupancy() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room room : roomRepository.findAll().stream().limit(10).collect(Collectors.toList())) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("roomNumber", room.getRoomNumber());
            entry.put("capacity", room.getCapacity());
            entry.put("occupied", room.getOccupiedBeds());
            entry.put("available", room.getAvailableBeds());
            result.add(entry);
        }
        return result;
    }
}

