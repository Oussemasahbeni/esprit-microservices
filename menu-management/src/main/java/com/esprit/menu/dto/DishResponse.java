package com.esprit.menu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record DishResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String photoUrl,
        boolean available,
        CategoryResponse category,
        Set<String> ingredients,
        Set<String> allergens,
        List<DishVariantResponse> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
