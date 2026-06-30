package com.esprit.reservation.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trimmed mirror of menu-management's GET /api/menus response — only the fields the
 * booking flow needs. Fetched live on every call, nothing is cached in this service.
 */
public record MenuSnapshot(
        List<Dish> dishes,
        List<Promotion> promotions
) {

    public record Dish(
            Long id,
            String name,
            BigDecimal price,
            boolean available,
            Category category
    ) {
    }

    public record Category(
            Long id,
            String name
    ) {
    }

    public record Promotion(
            Long id,
            String name,
            BigDecimal discountPercent,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            boolean active,
            Long dishId,
            Long categoryId
    ) {
    }
}
