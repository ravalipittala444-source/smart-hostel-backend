package com.example.Smart_Hostel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.DayOfWeek;

@Entity
@Table(name = "mess_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String items;

    @Column
    private String timing;

    public enum MealType { BREAKFAST, LUNCH, SNACKS, DINNER }
}

