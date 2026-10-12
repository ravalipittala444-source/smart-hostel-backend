package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.AppUser;
import com.example.Smart_Hostel.entity.Student;
import com.example.Smart_Hostel.repository.AppUserRepository;
import com.example.Smart_Hostel.repository.StudentRepository;
import com.example.Smart_Hostel.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AppUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String cleanEmail = req.getEmail().trim().toLowerCase();

        // 1. Validate email uniqueness in both users and students tables
        if (userRepository.existsByEmail(cleanEmail) || studentRepository.existsByEmail(cleanEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email address already exists."));
        }

        // 2. Validate password length
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 6 characters long."));
        }

        // 3. Strict Server-Side Role Enforcement
        AppUser.UserRole targetRole = AppUser.UserRole.STUDENT;
        if (req.getRole() != null && !req.getRole().isBlank()) {
            try {
                AppUser.UserRole requestedRole = AppUser.UserRole.valueOf(req.getRole().toUpperCase().trim());

                if (requestedRole == AppUser.UserRole.ADMIN || requestedRole == AppUser.UserRole.WARDEN) {
                    // Check if caller is authenticated as ADMIN
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth != null && auth.isAuthenticated() &&
                            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                    if (!isAdmin) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Public registration only permits STUDENT accounts. Staff roles (ADMIN/WARDEN) must be created by an authorized Administrator."));
                    }
                    targetRole = requestedRole;
                } else {
                    targetRole = requestedRole;
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid role specified. Valid roles: STUDENT, WARDEN, ADMIN."));
            }
        }

        // 4. Save new user with BCrypt password hash into users table
        String encodedPassword = passwordEncoder.encode(req.getPassword());
        AppUser newUser = AppUser.builder()
                .fullName(req.getFullName().trim())
                .name(req.getFullName().trim())
                .email(cleanEmail)
                .phone(req.getPhone() != null ? req.getPhone().trim() : "")
                .role(targetRole)
                .status(AppUser.UserStatus.ACTIVE)
                .passwordHash(encodedPassword)
                .password(encodedPassword)
                .createdAt(LocalDateTime.now())
                .build();

        AppUser savedUser = userRepository.save(newUser);

        // 5. If role is STUDENT, also create corresponding Student record in students table
        if (targetRole == AppUser.UserRole.STUDENT) {
            Student student = Student.builder()
                    .fullName(savedUser.getFullName())
                    .email(cleanEmail)
                    .phone(savedUser.getPhone() != null && !savedUser.getPhone().isBlank() ? savedUser.getPhone() : "N/A")
                    .gender(Student.Gender.OTHER)
                    .course("Hostel Resident")
                    .yearOfStudy(1)
                    .status(Student.StudentStatus.ACTIVE)
                    .build();
            studentRepository.save(student);
            log.info("Student record created in students table for: {}", cleanEmail);
        }

        log.info("User registered and saved to MySQL users table: id={}, email={}, role={}", 
                savedUser.getId(), cleanEmail, targetRole);

        // 6. Response WITHOUT password hash
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Account registered successfully. Please sign in.");
        response.put("id", savedUser.getId());
        response.put("fullName", savedUser.getFullName());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String cleanEmail = req.getEmail().trim().toLowerCase();

        AppUser user = userRepository.findByEmail(cleanEmail).orElse(null);

        // Verify user exists and BCrypt password matches
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email address or password."));
        }

        // Check if account is suspended
        if (user.getStatus() == AppUser.UserStatus.SUSPENDED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Your account has been suspended. Please contact the administrator."));
        }

        // Generate genuine JWT token
        String token = jwtService.generateToken(user);

        // Structured user profile (no password)
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("id", user.getId());
        userDetails.put("fullName", user.getFullName());
        userDetails.put("email", user.getEmail());
        userDetails.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userDetails.put("role", user.getRole().name());
        userDetails.put("status", user.getStatus().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
        response.put("role", user.getRole().name());
        response.put("status", user.getStatus().name());
        response.put("user", userDetails);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = null;
        if (auth.getPrincipal() instanceof AppUser) {
            email = ((AppUser) auth.getPrincipal()).getEmail();
        } else if (auth.getName() != null) {
            email = auth.getName();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unable to determine authenticated user"));
        }

        return userRepository.findByEmail(email).map(user -> {
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("fullName", user.getFullName());
            userDetails.put("email", user.getEmail());
            userDetails.put("phone", user.getPhone() != null ? user.getPhone() : "");
            userDetails.put("role", user.getRole().name());
            userDetails.put("status", user.getStatus().name());
            return ResponseEntity.ok((Object) userDetails);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found")));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "Smart Hostel API", "auth", "JWT-BCrypt");
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String role;
    }
}
