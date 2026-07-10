package com.esprit.delivery.service.implementation;

import com.esprit.delivery.dto.DeliveryDtos.AssignDriverRequest;
import com.esprit.delivery.entity.DeliveryAssignment;
import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.DriverAvailabilityStatus;
import com.esprit.delivery.enums.OrderStatus;
import com.esprit.delivery.exception.ApplicationException;
import com.esprit.delivery.exception.ErrorCode;
import com.esprit.delivery.messaging.events.DriverAssignedEvent;
import com.esprit.delivery.repository.DeliveryAssignmentRepository;
import com.esprit.delivery.repository.DriverRepository;
import com.esprit.delivery.repository.OrderRepository;
import com.esprit.delivery.service.DriverAssignmentService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link DriverAssignmentService}.
 *
 * <p>NOTE on assumptions made to fill gaps not present in the provided code:
 *
 * <ul>
 *   <li>{@code DeliveryAssignmentRepository} is assumed to expose {@code
 *       findByOrder_IdAndActiveTrue} and {@code findByOrder_Id}.
 *   <li>Nearest-driver search uses the driver's current lat/lng versus the order's {@code
 *       deliveryAddress} coordinates as a stand-in "pickup point", since {@link Order} has no
 *       separate restaurant/pickup field. Swap in the real pickup coordinates if/when that field
 *       exists.
 *   <li>RabbitMQ publishing is done via {@link RabbitTemplate} against a {@code delivery.exchange}
 *       exchange with routing key {@code driver.assigned}. Adjust names to match your actual
 *       config.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DriverAssignmentServiceImpl implements DriverAssignmentService {

  private static final String DELIVERY_EXCHANGE = "delivery.exchange";
  private static final String DRIVER_ASSIGNED_ROUTING_KEY = "driver.assigned";

  private final DeliveryAssignmentRepository deliveryAssignmentRepository;
  private final OrderRepository orderRepository;
  private final DriverRepository driverRepository;
  private final RabbitTemplate rabbitTemplate;

  @Override
  public DeliveryAssignment assignDriverToOrder(AssignDriverRequest request) {
    if (request.getDriverId() == null) {
      return autoAssignNearestDriver(request.getOrderId());
    }

    Order order = findOrderOrThrow(request.getOrderId());
    Driver driver = findDriverOrThrow(request.getDriverId());

    ensureOrderIsUnassigned(order);
    ensureDriverIsAvailable(driver);

    return createAssignment(order, driver);
  }

  @Override
  public DeliveryAssignment autoAssignNearestDriver(Long orderId) {
    Order order = findOrderOrThrow(orderId);
    ensureOrderIsUnassigned(order);

    List<Driver> availableDrivers =
        driverRepository.findByAvailabilityStatus(DriverAvailabilityStatus.AVAILABLE);
    if (availableDrivers.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.NO_AVAILABLE_DRIVERS, "No available drivers to assign to order: " + orderId);
    }

    Driver nearest =
        findNearestDriver(order, availableDrivers)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.NO_AVAILABLE_DRIVERS,
                        "No available drivers with a known position to assign to order: "
                            + orderId));

    return createAssignment(order, nearest);
  }

  @Override
  public DeliveryAssignment reassignDriver(Long orderId, Long newDriverId) {
    Order order = findOrderOrThrow(orderId);
    Driver newDriver = findDriverOrThrow(newDriverId);
    ensureDriverIsAvailable(newDriver);

    Optional<DeliveryAssignment> currentAssignment =
        deliveryAssignmentRepository.findByOrder_IdAndActiveTrue(orderId);

    currentAssignment.ifPresent(
        assignment -> {
          assignment.setActive(false);
          deliveryAssignmentRepository.save(assignment);

          // Free up the previous driver, unless they've already gone offline/inactive elsewhere.
          Driver previousDriver = assignment.getDriver();
          if (previousDriver.getAvailabilityStatus() == DriverAvailabilityStatus.BUSY) {
            previousDriver.setAvailabilityStatus(DriverAvailabilityStatus.AVAILABLE);
            driverRepository.save(previousDriver);
          }
        });

    return createAssignment(order, newDriver);
  }

  @Override
  @Transactional(readOnly = true)
  public DeliveryAssignment getActiveAssignment(Long orderId) {
    findOrderOrThrow(orderId); // 404s early if the order doesn't exist
    return deliveryAssignmentRepository
        .findByOrder_IdAndActiveTrue(orderId)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.ASSIGNMENT_NOT_FOUND,
                    "No active assignment found for order: " + orderId));
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  private DeliveryAssignment createAssignment(Order order, Driver driver) {
    DeliveryAssignment assignment =
        DeliveryAssignment.builder().order(order).driver(driver).active(true).build();

    DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);

    order.setStatus(OrderStatus.ASSIGNED);
    orderRepository.save(order);

    driver.setAvailabilityStatus(DriverAvailabilityStatus.BUSY);
    driverRepository.save(driver);

    publishDriverAssignedEvent(saved);

    return saved;
  }

  private void publishDriverAssignedEvent(DeliveryAssignment assignment) {
    DriverAssignedEvent event =
        new DriverAssignedEvent(
            assignment.getId(),
            assignment.getOrder().getId(),
            assignment.getDriver().getId(),
            assignment.getAssignedAt());

    try {
      rabbitTemplate.convertAndSend(DELIVERY_EXCHANGE, DRIVER_ASSIGNED_ROUTING_KEY, event);
    } catch (Exception ex) {
      log.error(
          "Failed to publish driver.assigned event for assignment {}: {}",
          assignment.getId(),
          ex.getMessage(),
          ex);
    }
  }

  private void ensureOrderIsUnassigned(Order order) {
    deliveryAssignmentRepository
        .findByOrder_IdAndActiveTrue(order.getId())
        .ifPresent(
            a -> {
              throw new ApplicationException(
                  ErrorCode.INVALID_STATE,
                  "Order " + order.getId() + " already has an active driver assignment");
            });
  }

  private void ensureDriverIsAvailable(Driver driver) {
    if (driver.getAvailabilityStatus() != DriverAvailabilityStatus.AVAILABLE) {
      throw new ApplicationException(
          ErrorCode.DRIVER_NOT_AVAILABLE,
          "Driver " + driver.getId() + " is not available for assignment");
    }
  }

  private Optional<Driver> findNearestDriver(Order order, List<Driver> candidates) {
    Double pickupLat =
        order.getDeliveryAddress() != null ? order.getDeliveryAddress().getLatitude() : null;
    Double pickupLng =
        order.getDeliveryAddress() != null ? order.getDeliveryAddress().getLongitude() : null;

    if (pickupLat == null || pickupLng == null) {
      // No pickup coordinates to compare against; fall back to the first
      // available candidate rather than failing the whole operation.
      return candidates.stream().findFirst();
    }

    return candidates.stream()
        .filter(d -> d.getCurrentLatitude() != null && d.getCurrentLongitude() != null)
        .min(
            (a, b) ->
                Double.compare(
                    haversineDistanceKm(
                        pickupLat, pickupLng, a.getCurrentLatitude(), a.getCurrentLongitude()),
                    haversineDistanceKm(
                        pickupLat, pickupLng, b.getCurrentLatitude(), b.getCurrentLongitude())));
  }

  private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
    final int earthRadiusKm = 6371;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }

  private Order findOrderOrThrow(Long orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.ORDER_NOT_FOUND, "Order with id: " + orderId + " was not found"));
  }

  private Driver findDriverOrThrow(Long driverId) {
    return driverRepository
        .findById(driverId)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.USER_NOT_FOUND, "Driver with id: " + driverId + " was not found"));
  }
}
