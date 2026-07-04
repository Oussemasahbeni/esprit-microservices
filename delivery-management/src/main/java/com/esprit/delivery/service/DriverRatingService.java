package com.esprit.delivery.service;

import com.esprit.delivery.dto.DeliveryDtos.RateDriverRequest;
import com.esprit.delivery.entity.DriverRating;

import java.util.List;

/**
 * Use cases for customers rating drivers after a delivery, and for exposing
 * aggregated rating data (consumed by {@code DriverService#getDriverDashboard}
 * and by MS5 - Analytics & Reporting Service).
 */
public interface DriverRatingService {

    /**
     * Records a rating for the driver of a completed order. Only allowed once
     * per order, and only after the order has reached DELIVERED status.
     * Triggers a recomputation of the driver's {@code averageRating}.
     */
    DriverRating rateDriver(RateDriverRequest request);

    /**
     * Lists all ratings received by a given driver.
     */
    List<DriverRating> getRatingsByDriver(Long driverId);

    /**
     * Computes (or returns the cached) average rating for a driver.
     */
    double getDriverAverageRating(Long driverId);
}
