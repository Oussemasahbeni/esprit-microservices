package com.esprit.reservation.messaging;

import java.time.LocalDateTime;

/**
 * Mirrors menu-management's DishUnavailableEvent wire format (dish.unavailable).
 */
public record DishUnavailableEvent(
        String eventId,
        String eventType,
        Long dishId,
        String dishName,
        Long categoryId,
        String categoryName,
        LocalDateTime occurredAt
) {
}
