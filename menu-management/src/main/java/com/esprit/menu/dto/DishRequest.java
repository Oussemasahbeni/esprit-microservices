package com.esprit.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record DishRequest(
        @NotBlank @Size(max = 140) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Size(max = 500) String photoUrl,
        boolean available,
        @NotNull Long categoryId,
        Set<@Size(max = 120) String> ingredients,
        Set<@Size(max = 120) String> allergens,
        List<@Valid DishVariantRequest> variants
) {
}
