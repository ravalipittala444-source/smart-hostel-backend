package com.example.Smart_Hostel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Synchronize with legacy 'name' column if present
    @Column(name = "name")
    private String name;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    // Synchronize with legacy 'password' column if present
    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void syncFields() {
        if (this.name == null && this.fullName != null) {
            this.name = this.fullName;
        }
        if (this.password == null && this.passwordHash != null) {
            this.password = this.passwordHash;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum UserRole { ADMIN, WARDEN, STUDENT }
    public enum UserStatus { ACTIVE, SUSPENDED }
}
