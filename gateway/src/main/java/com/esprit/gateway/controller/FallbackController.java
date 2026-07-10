package com.esprit.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static java.time.Instant.now;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/employee-management")
    public ResponseEntity<Map<String, Object>> employeeManagementFallback() {
        return buildFallback("employee-management", "Employee management service is currently unavailable");
    }

    @GetMapping("/menu-management")
    public ResponseEntity<Map<String, Object>> menuManagementFallback() {
        return buildFallback("menu-management", "Menu management service is currently unavailable");
    }

    @GetMapping("/delivery-management")
    public ResponseEntity<Map<String, Object>> deliveryManagementFallback() {
        return buildFallback("delivery-management", "Delivery management service is currently unavailable");
    }

    @GetMapping("/reservation")
    public ResponseEntity<Map<String, Object>> reservationFallback() {
        return buildFallback("reservation", "Reservation service is currently unavailable");
    }

    @GetMapping("/ai-service")
    public ResponseEntity<Map<String, Object>> aiServiceFallback() {
        return buildFallback("ai-service", "AI service is currently unavailable");
    }

    private ResponseEntity<Map<String, Object>> buildFallback(String service, String message) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", now().toString(),
                        "status", 503,
                        "error", "Service Unavailable",
                        "service", service,
                        "message", message
                ));
    }
}