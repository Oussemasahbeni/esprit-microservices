package com.esprit.menu.dto;

import java.util.List;

public record MenuResponse(
        MenuSummaryResponse summary,
        List<CategoryResponse> categories,
        List<DishResponse> dishes,
        List<PromotionResponse> promotions
) {
}
