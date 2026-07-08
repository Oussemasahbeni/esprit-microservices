package com.esprit.delivery.controller;

import com.esprit.delivery.dto.DeliveryDtos.AssignDriverRequest;
import com.esprit.delivery.entity.DeliveryAssignment;
import com.esprit.delivery.service.DriverAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST endpoints for assigning (and reassigning) drivers to orders. */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DriverAssignmentController {

  private final DriverAssignmentService driverAssignmentService;

  /**
   * Assigns a driver to an order. If {@code request.getDriverId()} is null, the service
   * auto-selects the nearest available driver.
   */
  @PostMapping("/assign-driver")
  public ResponseEntity<DeliveryAssignment> assignDriverToOrder(
      @Valid @RequestBody AssignDriverRequest request) {
    DeliveryAssignment assignment = driverAssignmentService.assignDriverToOrder(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
  }

  /** Automatically assigns the nearest available driver to the given order. */
  @PostMapping("/orders/{orderId}/auto-assign")
  public ResponseEntity<DeliveryAssignment> autoAssignNearestDriver(@PathVariable Long orderId) {
    DeliveryAssignment assignment = driverAssignmentService.autoAssignNearestDriver(orderId);
    return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
  }

  /** Replaces the driver currently assigned to an order with a new one. */
  @PutMapping("/orders/{orderId}/reassign")
  public ResponseEntity<DeliveryAssignment> reassignDriver(
      @PathVariable Long orderId, @RequestParam Long newDriverId) {
    DeliveryAssignment assignment = driverAssignmentService.reassignDriver(orderId, newDriverId);
    return ResponseEntity.ok(assignment);
  }

  /** Retrieves the currently active assignment for an order. */
  @GetMapping("/orders/{orderId}/assignment")
  public ResponseEntity<DeliveryAssignment> getActiveAssignment(@PathVariable Long orderId) {
    DeliveryAssignment assignment = driverAssignmentService.getActiveAssignment(orderId);
    return ResponseEntity.ok(assignment);
  }
}
