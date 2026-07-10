package com.esprit.delivery.repository;

import com.esprit.delivery.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByCustomerId(String customerId);
}
