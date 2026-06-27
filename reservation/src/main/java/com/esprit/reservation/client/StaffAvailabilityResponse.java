package com.esprit.reservation.client;

import java.time.LocalDateTime;

public record StaffAvailabilityResponse(
        LocalDateTime dateTime,
        int availableStaff,
        boolean sufficient
) {}
