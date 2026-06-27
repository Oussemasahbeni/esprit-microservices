package com.esprit.employee.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduleUpdatedEvent(
        String eventId,
        LocalDate date,
        int availableStaff,
        String updatedBy,
        LocalDateTime occurredAt
) {}
