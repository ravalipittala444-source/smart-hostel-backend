package com.example.Smart_Hostel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootHealthController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "service", "Smart Hostel Management System API",
            "status", "UP",
            "health", "/api/auth/health"
        );
    }

    @GetMapping({"/health", "/api/health"})
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api")
    public Map<String, Object> apiRoot() {
        return Map.of(
            "service", "Smart Hostel API Root",
            "status", "UP",
            "health", "/api/auth/health"
        );
    }
}

