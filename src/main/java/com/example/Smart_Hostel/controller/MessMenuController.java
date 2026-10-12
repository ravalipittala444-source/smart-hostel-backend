package com.example.Smart_Hostel.controller;

import com.example.Smart_Hostel.entity.MessMenu;
import com.example.Smart_Hostel.repository.MessMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mess")
@RequiredArgsConstructor
public class MessMenuController {

    private final MessMenuRepository messMenuRepository;

    @GetMapping
    public List<MessMenu> getAllMenus(
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String mealType) {

        if (day != null) {
            try {
                return messMenuRepository.findByDay(DayOfWeek.valueOf(day.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (mealType != null) {
            try {
                return messMenuRepository.findByMealType(MessMenu.MealType.valueOf(mealType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return messMenuRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessMenu> getMenuById(@PathVariable Long id) {
        return messMenuRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MessMenu> createMenu(@Valid @RequestBody MessMenu menu) {
        return ResponseEntity.ok(messMenuRepository.save(menu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessMenu> updateMenu(@PathVariable Long id, @Valid @RequestBody MessMenu updated) {
        return messMenuRepository.findById(id).map(menu -> {
            menu.setDay(updated.getDay());
            menu.setMealType(updated.getMealType());
            menu.setItems(updated.getItems());
            menu.setTiming(updated.getTiming());
            return ResponseEntity.ok(messMenuRepository.save(menu));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        if (!messMenuRepository.existsById(id)) return ResponseEntity.notFound().build();
        messMenuRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

