package com.esprit.delivery.service;

import com.esprit.delivery.entity.Customer;
import com.esprit.delivery.entity.DeliveryAddress;

public interface CustomerService {

  /**
   * Returns the cached profile for a customer, validating identity against the employee/IAM service
   * and lazily creating the cache row on first contact (typically the customer's first order).
   */
  Customer getOrCreateCustomer(String customerId);

  Customer getByCustomerId(String customerId);

  Customer updateDefaultAddress(String customerId, DeliveryAddress address);

  Customer addLoyaltyPoints(String customerId, int points);

  void recordOrderPlaced(String customerId);
}
