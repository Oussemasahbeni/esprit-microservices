package com.esprit.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Locally-cached view of the menu-management catalog, built entirely from RabbitMQ events
 * (dish.created/updated/deleted, promo.created) — no synchronous call to menu-management.
 */
public record MenuSnapshotResponse(
        List<DishOption> availableDishes,
        List<PromotionOption> activePromotions
) {
    public record DishOption(Long id, String name, BigDecimal price, String categoryName) {
    }

    public record PromotionOption(Long id, String name, BigDecimal discountPercent, Long dishId,
                                   Long categoryId, LocalDateTime startsAt, LocalDateTime endsAt) {
    }
}
