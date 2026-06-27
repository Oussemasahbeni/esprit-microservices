package com.esprit.menu.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank @Size(max = 140) String name,
        @Size(max = 700) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal discountPercent,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        boolean active,
        Long dishId,
        Long categoryId
) {
    @AssertTrue(message = "Either dishId or categoryId is required")
    public boolean hasTarget() {
        return dishId != null || categoryId != null;
    }

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean hasValidWindow() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
