package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.AppUser;
import com.example.Smart_Hostel.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<AppUser> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        if (search != null && !search.isBlank()) {
            return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        }
        if (role != null) {
            try {
                return userRepository.findByRole(AppUser.UserRole.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase().trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        String initialPassword = (req.getPassword() != null && !req.getPassword().isBlank()) 
                ? req.getPassword() : "HostelUser@2026!";

        AppUser user = AppUser.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail().toLowerCase().trim())
                .phone(req.getPhone())
                .role(req.getRole() != null ? req.getRole() : AppUser.UserRole.STUDENT)
                .status(req.getStatus() != null ? req.getStatus() : AppUser.UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(initialPassword))
                .createdAt(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest req) {
        return userRepository.findById(id).map(user -> {
            if (req.getFullName() != null) user.setFullName(req.getFullName().trim());
            if (req.getPhone() != null) user.setPhone(req.getPhone());
            if (req.getRole() != null) user.setRole(req.getRole());
            if (req.getStatus() != null) user.setStatus(req.getStatus());
            if (req.getPassword() != null && !req.getPassword().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            }
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userRepository.findById(id).map(user -> {
            try {
                user.setStatus(AppUser.UserStatus.valueOf(body.get("status").toUpperCase()));
                return ResponseEntity.ok(userRepository.save(user));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    @lombok.Data
    public static class UserRequest {
        private String fullName;
        @jakarta.validation.constraints.Email
        private String email;
        private String phone;
        private String password;
        private AppUser.UserRole role;
        private AppUser.UserStatus status;
    }
}
