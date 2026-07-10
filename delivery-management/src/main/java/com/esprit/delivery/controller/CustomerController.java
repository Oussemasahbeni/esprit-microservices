package com.esprit.delivery.controller;

import com.esprit.delivery.entity.Customer;
import com.esprit.delivery.entity.DeliveryAddress;
import com.esprit.delivery.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @GetMapping("/{customerId}")
  public Customer getByCustomerId(@PathVariable String customerId) {
    return customerService.getByCustomerId(customerId);
  }

  @PutMapping("/{customerId}/default-address")
  public Customer updateDefaultAddress(
      @PathVariable String customerId, @RequestBody DeliveryAddress address) {
    return customerService.updateDefaultAddress(customerId, address);
  }
}
