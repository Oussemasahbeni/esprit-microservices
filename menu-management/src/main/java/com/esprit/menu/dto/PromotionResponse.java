package com.esprit.menu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
        Long id,
        String name,
        String description,
        BigDecimal discountPercent,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        Long dishId,
        String dishName,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
