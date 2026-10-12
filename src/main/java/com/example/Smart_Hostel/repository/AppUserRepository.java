package com.example.Smart_Hostel.repository;

import com.example.Smart_Hostel.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AppUser> findByRole(AppUser.UserRole role);
    List<AppUser> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}

