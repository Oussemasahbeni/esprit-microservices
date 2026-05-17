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

    @GetMapping("/demo1")
    public ResponseEntity<Map<String, Object>> demo1Fallback() {
        return buildFallback("demo1", "User service is currently unavailable");
    }

    @GetMapping("/demo2")
    public ResponseEntity<Map<String, Object>> demo2Fallback() {
        return buildFallback("demo2", "Product service is currently unavailable");
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