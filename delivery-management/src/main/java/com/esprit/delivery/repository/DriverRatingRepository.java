package com.esprit.delivery.repository;

import com.esprit.delivery.entity.DriverRating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRatingRepository extends JpaRepository<DriverRating, Long> {

    List<DriverRating> findByDriver_Id(Long driverId);

    boolean existsByOrder_Id(Long orderId);
}
