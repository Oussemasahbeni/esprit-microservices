package com.esprit.menu.dto;

public record MenuSummaryResponse(
        long totalDishes,
        long availableDishes,
        long unavailableDishes,
        long categories,
        long activePromotions
) {
}
