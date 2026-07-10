package com.esprit.delivery.messaging.events;

import java.time.LocalDateTime;

public record DriverAssignedEvent(
    Long assignmentId, Long orderId, Long driverId, LocalDateTime assignedAt) {}
