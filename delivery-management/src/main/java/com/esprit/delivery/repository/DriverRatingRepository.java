package com.esprit.delivery.repository;

import com.esprit.delivery.entity.DriverRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRatingRepository extends JpaRepository<DriverRating, Long> {

    List<DriverRating> findByDriver_Id(Long driverId);
}
