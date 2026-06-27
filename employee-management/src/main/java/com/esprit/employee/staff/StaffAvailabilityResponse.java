package com.esprit.employee.staff;

import java.time.LocalDateTime;

public record StaffAvailabilityResponse(
        LocalDateTime dateTime,
        int availableStaff,
        boolean sufficient
) {}
