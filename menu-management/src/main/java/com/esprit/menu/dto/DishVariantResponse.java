package com.esprit.menu.dto;

import java.math.BigDecimal;

public record DishVariantResponse(
        Long id,
        String name,
        BigDecimal priceDelta,
        boolean available
) {
}
