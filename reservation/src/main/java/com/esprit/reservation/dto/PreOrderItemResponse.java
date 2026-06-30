package com.esprit.reservation.dto;

import java.math.BigDecimal;

public record PreOrderItemResponse(
        Long menuDishId,
        String dishName,
        BigDecimal unitPrice,
        Integer quantity,
        boolean stillAvailable
) {
}
