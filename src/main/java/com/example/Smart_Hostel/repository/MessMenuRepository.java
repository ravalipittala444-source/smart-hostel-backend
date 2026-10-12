package com.example.Smart_Hostel.repository;

import com.example.Smart_Hostel.entity.MessMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface MessMenuRepository extends JpaRepository<MessMenu, Long> {
    List<MessMenu> findByDay(DayOfWeek day);
    List<MessMenu> findByMealType(MessMenu.MealType mealType);
}

