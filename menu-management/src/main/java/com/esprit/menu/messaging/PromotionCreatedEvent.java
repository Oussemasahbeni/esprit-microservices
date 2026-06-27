package com.esprit.menu.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionCreatedEvent(
        String eventId,
        String eventType,
        Long promotionId,
        String promotionName,
        BigDecimal discountPercent,
        Long dishId,
        Long categoryId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime occurredAt
) {
}
