package com.esprit.delivery.dto;

import com.esprit.delivery.enums.DriverAvailabilityStatus;
import com.esprit.delivery.enums.VehicleType;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTOs consumed by {@code DriverController}.
 * <p>
 * NOTE: if your project's {@code DeliveryDtos} class (referenced in
 * DriverService for {@code UpdateDriverLocationRequest}) already exists,
 * just add these two records as nested types inside it instead of keeping
 * this separate file, so everything lives in one place.
 */
public class DriverControllerDtos {

    public record RegisterDriverRequest(
            @NotNull Long employeeId,
            @NotNull VehicleType vehicleType
    ) {
    }

    public record UpdateAvailabilityRequest(
            @NotNull DriverAvailabilityStatus status
    ) {
    }
}