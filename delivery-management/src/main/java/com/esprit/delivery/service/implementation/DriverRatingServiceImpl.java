package com.esprit.delivery.service.implementation;

import com.esprit.delivery.dto.DeliveryDtos.RateDriverRequest;
import com.esprit.delivery.entity.Driver;
import com.esprit.delivery.entity.DriverRating;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.OrderStatus;
import com.esprit.delivery.exception.ApplicationException;
import com.esprit.delivery.exception.ErrorCode;
import com.esprit.delivery.repository.DriverRatingRepository;
import com.esprit.delivery.repository.DriverRepository;
import com.esprit.delivery.repository.OrderRepository;
import com.esprit.delivery.service.DriverRatingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link DriverRatingService}.
 *
 * <p>NOTE on assumptions made to fill gaps not present in the provided code:
 *
 * <ul>
 *   <li>{@code DriverRatingRepository} is assumed to expose {@code existsByOrder_Id}, {@code
 *       findByOrder_Id} and {@code findByDriver_Id}.
 *   <li>{@code RateDriverRequest} is assumed to expose {@code orderId}, {@code customerId}, {@code
 *       score}, and {@code comment}.
 *   <li>The rating customer is assumed to be validated as the order's {@code customerId} owner;
 *       adjust if authorization is handled elsewhere (e.g. via a security context / gateway).
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DriverRatingServiceImpl implements DriverRatingService {

  private final DriverRatingRepository driverRatingRepository;
  private final DriverRepository driverRepository;
  private final OrderRepository orderRepository;

  @Override
  public DriverRating rateDriver(RateDriverRequest request) {
    Order order = findOrderOrThrow(request.getOrderId());

    if (order.getStatus() != OrderStatus.DELIVERED) {
      throw new ApplicationException(
          ErrorCode.INVALID_STATE,
          "Order " + order.getId() + " has not been delivered yet and cannot be rated");
    }

    if (order.getAssignment() == null || order.getAssignment().getDriver() == null) {
      throw new ApplicationException(
          ErrorCode.INVALID_STATE, "Order " + order.getId() + " has no assigned driver to rate");
    }

    if (driverRatingRepository.existsByOrder_Id(order.getId())) {
      throw new ApplicationException(
          ErrorCode.INVALID_STATE, "Order " + order.getId() + " has already been rated");
    }

    validateScore(request.getScore());

    Driver driver = order.getAssignment().getDriver();

    DriverRating rating =
        DriverRating.builder()
            .driver(driver)
            .order(order)
            .customerId(request.getCustomerId())
            .score(request.getScore())
            .comment(request.getComment())
            .build();

    DriverRating saved = driverRatingRepository.save(rating);

    recomputeAverageRating(driver);

    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverRating> getRatingsByDriver(Long driverId) {
    findDriverOrThrow(driverId);
    return driverRatingRepository.findByDriver_Id(driverId);
  }

  @Override
  @Transactional(readOnly = true)
  public double getDriverAverageRating(Long driverId) {
    Driver driver = findDriverOrThrow(driverId);
    return driver.getAverageRating() != null ? driver.getAverageRating() : 0.0;
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  private void recomputeAverageRating(Driver driver) {
    List<DriverRating> ratings = driverRatingRepository.findByDriver_Id(driver.getId());

    double average = ratings.stream().mapToInt(DriverRating::getScore).average().orElse(0.0);

    driver.setAverageRating(average);
    driverRepository.save(driver);
  }

  private void validateScore(Integer score) {
    if (score == null || score < 1 || score > 5) {
      throw new ApplicationException(
          ErrorCode.INVALID_STATE, "Rating score must be between 1 and 5");
    }
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
