package com.esprit.delivery.service.implementation;

import static com.esprit.delivery.exception.ErrorCode.*;

import com.esprit.delivery.client.feign.EmployeeServiceClient;
import com.esprit.delivery.client.feign.dto.IdentityUserResponse;
import com.esprit.delivery.entity.Customer;
import com.esprit.delivery.entity.DeliveryAddress;
import com.esprit.delivery.exception.ApplicationException;
import com.esprit.delivery.repository.CustomerRepository;
import com.esprit.delivery.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepository customerRepository;
  private final EmployeeServiceClient identityServiceClient;

  @Override
  public Customer getOrCreateCustomer(String customerId) {
    return customerRepository
        .findByCustomerId(customerId)
        .orElseGet(() -> registerFromIdentity(customerId));
  }

  @Override
  @Transactional(readOnly = true)
  public Customer getByCustomerId(String customerId) {
    return customerRepository
        .findByCustomerId(customerId)
        .orElseThrow(
            () ->
                new ApplicationException(
                    USER_NOT_FOUND, "Customer with id: " + customerId + " was not found"));
  }

  @Override
  public Customer updateDefaultAddress(String customerId, DeliveryAddress address) {
    Customer customer = getOrCreateCustomer(customerId);
    customer.setDefaultDeliveryAddress(address);
    return customerRepository.save(customer);
  }

  @Override
  public Customer addLoyaltyPoints(String customerId, int points) {
    Customer customer = getOrCreateCustomer(customerId);
    customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
    return customerRepository.save(customer);
  }

  @Override
  public void recordOrderPlaced(String customerId) {
    Customer customer = getOrCreateCustomer(customerId);
    customer.setTotalOrders(customer.getTotalOrders() + 1);
    customerRepository.save(customer);
  }

  /**
   * First contact with this customerId: confirm the identity is real and active via the
   * employee/IAM service, then create the local cache row.
   */
  private Customer registerFromIdentity(String customerId) {
    IdentityUserResponse identity = identityServiceClient.getById(customerId);
    if (identity == null) {
      throw new ApplicationException(
          USER_NOT_FOUND, "Customer with id: " + customerId + " was not found");
    }
    if (!identity.isActive()) {
      throw new ApplicationException(
          ORDER_NOT_ALLOWED, "Customer account " + customerId + " is not active");
    }
    if (!identity.isCustomer()) {
      throw new ApplicationException(
          ORDER_NOT_ALLOWED, "User " + customerId + " does not hold the CUSTOMER role");
    }

    Customer customer = Customer.builder().customerId(customerId).build();
    log.info("Customer {} seen for the first time by delivery-ms; cached locally", customerId);
    return customerRepository.save(customer);
  }
}
