package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.Complaint;
import com.example.Smart_Hostel.entity.Student;
import com.example.Smart_Hostel.repository.ComplaintRepository;
import com.example.Smart_Hostel.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public List<Complaint> getAllComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        if (status != null) {
            try {
                return complaintRepository.findByStatus(Complaint.ComplaintStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (category != null) {
            try {
                return complaintRepository.findByCategory(Complaint.ComplaintCategory.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return complaintRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getComplaintById(@PathVariable Long id) {
        return complaintRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createComplaint(@Valid @RequestBody ComplaintRequest req) {
        Complaint complaint = Complaint.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : Complaint.ComplaintCategory.OTHER)
                .status(Complaint.ComplaintStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        if (req.getStudentId() != null) {
            studentRepository.findById(req.getStudentId()).ifPresent(complaint::setStudent);
        }
        return ResponseEntity.ok(complaintRepository.save(complaint));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return complaintRepository.findById(id).map(complaint -> {
            try {
                complaint.setStatus(Complaint.ComplaintStatus.valueOf(body.get("status").toUpperCase()));
                if (complaint.getStatus() == Complaint.ComplaintStatus.RESOLVED) {
                    complaint.setResolvedAt(LocalDateTime.now());
                    complaint.setResolution(body.get("resolution"));
                }
                return ResponseEntity.ok(complaintRepository.save(complaint));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        if (!complaintRepository.existsById(id)) return ResponseEntity.notFound().build();
        complaintRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public Map<String, Long> getComplaintStats() {
        return Map.of(
                "total", complaintRepository.count(),
                "pending", complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING),
                "inProgress", complaintRepository.countByStatus(Complaint.ComplaintStatus.IN_PROGRESS),
                "resolved", complaintRepository.countByStatus(Complaint.ComplaintStatus.RESOLVED)
        );
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    @lombok.Data
    public static class ComplaintRequest {
        @jakarta.validation.constraints.NotBlank
        private String title;
        @jakarta.validation.constraints.NotBlank
        private String description;
        private Complaint.ComplaintCategory category;
        private Long studentId;
    }
}

