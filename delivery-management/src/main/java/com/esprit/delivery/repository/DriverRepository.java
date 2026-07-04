package com.esprit.delivery.repository;

import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.enums.DriverAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmployeeId(Long employeeId);

    List<Driver> findByAvailabilityStatus(DriverAvailabilityStatus status);
}
