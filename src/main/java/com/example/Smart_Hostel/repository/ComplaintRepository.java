package com.example.Smart_Hostel.repository;

import com.example.Smart_Hostel.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStatus(Complaint.ComplaintStatus status);
    List<Complaint> findByCategory(Complaint.ComplaintCategory category);
    List<Complaint> findByStudentId(Long studentId);
    long countByStatus(Complaint.ComplaintStatus status);
}

