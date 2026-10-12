package com.example.Smart_Hostel.repository;

import com.example.Smart_Hostel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumber(String roomNumber);
    List<Room> findByStatus(Room.RoomStatus status);
}

