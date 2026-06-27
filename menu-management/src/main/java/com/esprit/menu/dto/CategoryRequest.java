package com.esprit.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull Integer displayOrder,
        boolean active
) {
}
