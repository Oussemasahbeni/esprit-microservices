package com.esprit.delivery.entity;

import com.esprit.delivery.enums.DriverAvailabilityStatus;
import com.esprit.delivery.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a delivery driver within the Delivery Management Service.
 * <p>
 * The driver's HR profile (contract, salary, leaves...) is owned by MS1
 * (Employee Management Service). This entity only stores the operational
 * data needed for dispatching: real-time availability, vehicle, and current
 * position. The {@code employeeId} links back to MS1 and is validated
 * through {@code EmployeeServiceClient} (Feign) when a driver is registered.
 */
@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign reference to the employee record in MS1 (Employee Management Service).
     */
    @Column(nullable = false, unique = true)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverAvailabilityStatus availabilityStatus = DriverAvailabilityStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    /**
     * Last known position, updated periodically by the driver mobile app.
     */
    private Double currentLatitude;
    private Double currentLongitude;

    /**
     * Running average rating (1-5), recomputed whenever a new DriverRating is added.
     */
    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalDeliveries = 0;
}

