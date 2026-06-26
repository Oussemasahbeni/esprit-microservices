package com.esprit.menu.messaging;

import java.time.LocalDateTime;

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
