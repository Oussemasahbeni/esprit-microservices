package com.esprit.employee.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffAvailabilityController {

    private final StaffAvailabilityService staffAvailabilityService;

    @GetMapping("/availability")
    public StaffAvailabilityResponse checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        return staffAvailabilityService.checkAvailability(date, time);
    }
}
