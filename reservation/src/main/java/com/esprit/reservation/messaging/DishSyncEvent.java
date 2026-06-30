package com.esprit.reservation.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors menu-management's DishSyncEvent wire format (dish.created/dish.updated/dish.deleted).
 */
public record DishSyncEvent(
        String eventId,
        String eventType,
        Long dishId,
        String dishName,
        BigDecimal price,
        boolean available,
        Long categoryId,
        String categoryName,
        LocalDateTime occurredAt
) {
}
