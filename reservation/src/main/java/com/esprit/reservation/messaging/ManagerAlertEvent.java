package com.esprit.reservation.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ManagerAlertEvent(
        String eventId,
        String alertType,
        LocalDate date,
        int confirmedReservations,
        int availableStaff,
        int neededStaff,
        String message,
        LocalDateTime occurredAt
) {}
