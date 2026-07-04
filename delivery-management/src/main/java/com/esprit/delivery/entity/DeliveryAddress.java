package com.esprit.delivery.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Value object embedded in {@link Order}, representing the delivery destination.
 * Latitude/longitude are kept alongside the textual address to allow the
 * {@code DriverAssignmentService} to compute distances when auto-assigning drivers.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    private String street;
    private String city;
    private String postalCode;

    /** Optional free-text instructions for the driver (e.g. "ring the bell twice"). */
    private String additionalInfo;

    private Double latitude;
    private Double longitude;
}
