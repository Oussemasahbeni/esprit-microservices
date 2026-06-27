package com.esprit.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DishVariantRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("-9999.99") BigDecimal priceDelta,
        boolean available
) {
}
