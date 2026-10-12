package com.example.Smart_Hostel.security;

import com.example.Smart_Hostel.entity.AppUser;
import com.example.Smart_Hostel.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@smarthostel.edu}")
    private String adminEmail;

    @Value("${admin.password:Admin@Hostel2026!}")
    private String adminPassword;

    @Value("${admin.name:System Administrator}")
    private String adminName;

    @Value("${admin.phone:9876543210}")
    private String adminPhone;

    @Override
    public void run(String... args) {
        // Only provision if no ADMIN account exists in the database
        boolean adminExists = userRepository.findByRole(AppUser.UserRole.ADMIN).size() > 0;

        if (!adminExists) {
            // Check if user with that email already exists
            if (!userRepository.existsByEmail(adminEmail)) {
                String encodedPassword = passwordEncoder.encode(adminPassword);
                AppUser admin = AppUser.builder()
                        .fullName(adminName)
                        .name(adminName)
                        .email(adminEmail.toLowerCase().trim())
                        .phone(adminPhone)
                        .role(AppUser.UserRole.ADMIN)
                        .status(AppUser.UserStatus.ACTIVE)
                        .passwordHash(encodedPassword)
                        .password(encodedPassword)
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepository.save(admin);
                log.info("Secure Bootstrap: Initial administrator account provisioned for: {}", adminEmail);
            }
        }
    }
}

