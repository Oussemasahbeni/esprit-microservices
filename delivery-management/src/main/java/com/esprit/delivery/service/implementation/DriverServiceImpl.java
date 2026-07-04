package com.esprit.delivery.service.implementation;

import com.esprit.delivery.client.feign.EmployeeServiceClient;
import com.esprit.delivery.client.feign.dto.EmployeeResponse;
import com.esprit.delivery.dto.DeliveryDtos.UpdateDriverLocationRequest;
import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.DriverAvailabilityStatus;
import com.esprit.delivery.enums.VehicleType;
import com.esprit.delivery.exception.ApplicationException;
import com.esprit.delivery.exception.ErrorCode;
import com.esprit.delivery.repository.DriverRepository;
import com.esprit.delivery.repository.OrderRepository;
import com.esprit.delivery.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link DriverService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final EmployeeServiceClient employeeServiceClient;

    @Override
    public Driver registerDriver(Long employeeId, VehicleType vehicleType) {
        if (driverRepository.findByEmployeeId(employeeId).isPresent()) {
            throw new IllegalStateException(
                    "Employee " + employeeId + " is already registered as a driver");
        }

        EmployeeResponse employee = employeeServiceClient.getEmployeeById(employeeId);
        if (employee == null) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND, "Driver with id: " + employeeId + " was not found");
        }
        if (!employee.isActive()) {
            throw new IllegalStateException(
                    "Employee " + employeeId + " is not active and cannot be registered as a driver");
        }
        if (!employee.isDeliveryMan()) {
            throw new IllegalStateException(
                    "Employee " + employeeId + " does not hold the DELIVERY_MAN role and cannot be registered as a driver");
        }

        Driver driver = Driver.builder()
                .employeeId(employeeId)
                .vehicleType(vehicleType)
                .availabilityStatus(DriverAvailabilityStatus.OFFLINE)
                .averageRating(0.0)
                .totalDeliveries(0)
                .build();

        return driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public Driver getDriverById(Long driverId) {
        return findDriverOrThrow(driverId);
    }

    @Override
    public Driver updateAvailabilityStatus(Long driverId, DriverAvailabilityStatus status) {
        Driver driver = findDriverOrThrow(driverId);
        driver.setAvailabilityStatus(status);
        return driverRepository.save(driver);
    }

    @Override
    public void updateDriverLocation(UpdateDriverLocationRequest request) {
        Driver driver = findDriverOrThrow(request.getDriverId());
        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByAvailabilityStatus(DriverAvailabilityStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverDashboard getDriverDashboard(Long driverId) {
        Driver driver = findDriverOrThrow(driverId);
        List<Order> assignedOrders = orderRepository.findActiveOrdersByDriverId(driverId);

        return new DriverDashboard(
                driver,
                assignedOrders,
                driver.getTotalDeliveries(),
                driver.getAverageRating()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getDriverHistory(Long driverId) {
        findDriverOrThrow(driverId); // 404s early if the driver doesn't exist
        return orderRepository.findAllByDriverId(driverId);
    }

    private Driver findDriverOrThrow(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.USER_NOT_FOUND, "Driver with id: " + driverId + " was not found")
                );
    }
}