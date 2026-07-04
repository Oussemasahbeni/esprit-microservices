package com.esprit.delivery.service;

import com.esprit.delivery.dto.DeliveryDtos.AssignDriverRequest;
import com.esprit.delivery.entity.DeliveryAssignment;

/**
 * Use cases related to assigning (and reassigning) drivers to orders.
 * <p>
 * Implementations are expected to publish a {@code driver.assigned} event
 * (RabbitMQ) once an assignment is created, and to update the order's status
 * to ASSIGNED and the driver's availability to BUSY accordingly.
 */
public interface DriverAssignmentService {

    /**
     * Assigns a driver to an order (maps to {@code POST /assign-driver}).
     * If {@code request.getDriverId()} is null, delegates to
     * {@link #autoAssignNearestDriver(Long)} to pick the best candidate.
     */
    DeliveryAssignment assignDriverToOrder(AssignDriverRequest request);

    /**
     * Automatically selects the nearest AVAILABLE driver to the restaurant
     * (or to the order's pickup point) using current driver GPS positions,
     * and assigns them to the given order.
     */
    DeliveryAssignment autoAssignNearestDriver(Long orderId);

    /**
     * Replaces the driver currently assigned to an order (e.g. the original
     * driver became unreachable). Closes the previous assignment (active=false)
     * and creates a new active one, then publishes a new {@code driver.assigned} event.
     */
    DeliveryAssignment reassignDriver(Long orderId, Long newDriverId);

    /**
     * Retrieves the currently active assignment for a given order, if any.
     */
    DeliveryAssignment getActiveAssignment(Long orderId);
}
