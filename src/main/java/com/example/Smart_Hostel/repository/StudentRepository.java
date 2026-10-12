package com.example.Smart_Hostel.repository;

import com.example.Smart_Hostel.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByEmail(String email);
    Optional<Student> findByEmail(String email);
    List<Student> findByRoomId(Long roomId);
    List<Student> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    long countByStatus(Student.StudentStatus status);
}

