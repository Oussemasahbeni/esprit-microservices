package com.esprit.delivery.service;

import com.esprit.delivery.dto.DeliveryDtos.UpdateDriverLocationRequest;
import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.DriverAvailabilityStatus;
import com.esprit.delivery.enums.VehicleType;

import java.util.List;

/**
 * Use cases related to driver profile management and the driver dashboard.
 * The driver's HR data lives in MS1; this service only manages operational
 * (delivery-specific) driver data.
 */
public interface DriverService {

    /**
     * Registers an employee as a delivery driver. Validates the employee
     * exists and is active via {@code EmployeeServiceClient} (Feign) before
     * creating the local {@code Driver} record.
     */
    Driver registerDriver(Long employeeId, VehicleType vehicleType);

    /**
     * Retrieves a driver by its local id.
     */
    Driver getDriverById(Long driverId);

    /**
     * Updates a driver's availability (AVAILABLE / BUSY / ON_BREAK / OFFLINE),
     * typically toggled from the driver mobile app.
     */
    Driver updateAvailabilityStatus(Long driverId, DriverAvailabilityStatus status);

    /**
     * Updates a driver's last known GPS position (periodic ping from the mobile app).
     */
    void updateDriverLocation(UpdateDriverLocationRequest request);

    /**
     * Lists all drivers currently AVAILABLE (maps to {@code GET /drivers/available}).
     */
    List<Driver> getAvailableDrivers();

    /**
     * Builds the driver dashboard: currently assigned order(s), recent
     * delivery history, and aggregated stats (total deliveries, average rating).
     */
    DriverDashboard getDriverDashboard(Long driverId);

    /**
     * Returns the full delivery history (completed assignments) for a driver.
     */
    List<Order> getDriverHistory(Long driverId);

    /**
     * Lightweight read model returned to the driver mobile app's home screen.
     */
    record DriverDashboard(
            Driver driver,
            List<Order> assignedOrders,
            int totalDeliveries,
            double averageRating
    ) {
    }
}
