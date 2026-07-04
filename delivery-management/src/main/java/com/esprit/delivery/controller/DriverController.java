package com.esprit.delivery.controller;

import com.esprit.delivery.dto.DeliveryDtos.UpdateDriverLocationRequest;
import com.esprit.delivery.dto.DriverControllerDtos.RegisterDriverRequest;
import com.esprit.delivery.dto.DriverControllerDtos.UpdateAvailabilityRequest;
import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.service.DriverService;
import com.esprit.delivery.service.DriverService.DriverDashboard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for driver profile management and the driver mobile app dashboard.
 * Driver HR data (contract, salary, leaves...) lives in MS1 - this controller
 * only exposes the operational, delivery-specific driver data owned here.
 */
@RestController
@RequestMapping("/api/deliveries/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    /**
     * Registers an already-existing MS1 employee as a delivery driver.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Driver registerDriver(@Valid @RequestBody RegisterDriverRequest request) {
        return driverService.registerDriver(request.employeeId(), request.vehicleType());
    }

    @GetMapping("/{driverId}")
    public Driver getDriverById(@PathVariable Long driverId) {
        return driverService.getDriverById(driverId);
    }

    /**
     * Toggled from the driver mobile app (AVAILABLE / BUSY / ON_BREAK / OFFLINE).
     */
    @PatchMapping("/{driverId}/availability")
    public Driver updateAvailabilityStatus(
            @PathVariable Long driverId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return driverService.updateAvailabilityStatus(driverId, request.status());
    }

    /**
     * Periodic GPS ping from the driver mobile app.
     */
    @PostMapping("/location")
    public ResponseEntity<Void> updateDriverLocation(@Valid @RequestBody UpdateDriverLocationRequest request) {
        driverService.updateDriverLocation(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Drivers currently AVAILABLE, used by the auto-assignment flow / dispatch dashboard.
     */
    @GetMapping("/available")
    public List<Driver> getAvailableDrivers() {
        return driverService.getAvailableDrivers();
    }

    /**
     * Driver mobile app home screen: current assignment(s) + aggregated stats.
     */
    @GetMapping("/{driverId}/dashboard")
    public DriverDashboard getDriverDashboard(@PathVariable Long driverId) {
        return driverService.getDriverDashboard(driverId);
    }

    /**
     * Full delivery history for a driver.
     */
    @GetMapping("/{driverId}/history")
    public List<Order> getDriverHistory(@PathVariable Long driverId) {
        return driverService.getDriverHistory(driverId);
    }
}
