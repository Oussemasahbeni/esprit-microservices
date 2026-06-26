package com.esprit.employee.staff;

import java.time.LocalDate;
import java.time.LocalTime;

public record StaffAvailabilityResponse(
        LocalDate date,
        LocalTime time,
        int availableStaff,
        boolean sufficient
) {}
