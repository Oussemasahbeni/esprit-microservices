package com.esprit.delivery.controller;

import com.esprit.delivery.dto.DeliveryDtos.RateDriverRequest;
import com.esprit.delivery.entity.DriverRating;
import com.esprit.delivery.service.DriverRatingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for customers rating drivers, and for exposing aggregated
 * rating data.
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DriverRatingController {

    private final DriverRatingService driverRatingService;

    /**
     * Records a rating for the driver of a completed order.
     */
    @PostMapping("/ratings")
    public ResponseEntity<DriverRating> rateDriver(@Valid @RequestBody RateDriverRequest request) {
        DriverRating rating = driverRatingService.rateDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    /**
     * Lists all ratings received by a given driver.
     */
    @GetMapping("/drivers/{driverId}/ratings")
    public ResponseEntity<List<DriverRating>> getRatingsByDriver(@PathVariable Long driverId) {
        List<DriverRating> ratings = driverRatingService.getRatingsByDriver(driverId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Returns the (cached) average rating for a driver.
     */
    @GetMapping("/drivers/{driverId}/ratings/average")
    public ResponseEntity<Double> getDriverAverageRating(@PathVariable Long driverId) {
        double average = driverRatingService.getDriverAverageRating(driverId);
        return ResponseEntity.ok(average);
    }
}