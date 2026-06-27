package com.esprit.employee.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffAvailabilityController {

    private final StaffAvailabilityService staffAvailabilityService;

    @GetMapping("/availability")
    public StaffAvailabilityResponse checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        return staffAvailabilityService.checkAvailability(dateTime);
    }
}
