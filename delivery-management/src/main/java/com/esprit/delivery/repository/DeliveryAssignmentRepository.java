package com.esprit.delivery.repository;

import com.esprit.delivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findByOrder_IdAndActiveTrue(Long orderId);

    List<DeliveryAssignment> findByDriver_Id(Long driverId);
}
