package com.esprit.reservation.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationEvent(
        String eventId,
        String eventType,
        Long reservationId,
        String reservationCode,
        String customerEmail,
        String customerPhone,
        LocalDate reservationDate,
        LocalTime startTime,
        int guestsCount,
        String tableNumber,
        LocalDateTime occurredAt
) {}
