package com.esprit.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PreOrderItemRequest(
        @NotNull(message = "Dish id is required") Long dishId,
        @Min(value = 1, message = "Quantity must be at least 1") int quantity
) {
}
