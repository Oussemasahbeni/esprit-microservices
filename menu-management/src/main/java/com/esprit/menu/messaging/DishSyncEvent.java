package com.esprit.menu.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full catalog snapshot of a dish, published on every create/update/delete so that
 * downstream services (e.g. reservation) can keep a local read-model in sync without
 * a synchronous call back into menu-management.
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
