package com.esprit.delivery.client.feign;

import com.esprit.delivery.client.feign.dto.EmployeeResponse;
import com.esprit.delivery.client.feign.dto.IdentityUserResponse;
import com.esprit.delivery.config.FeignClientAuthConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Synchronous client used to call MS1 (Employee Management Service).
 *
 * <p>Used by {@code DriverService#registerDriver} to confirm that the given employeeId exists and
 * is active before it can operate as a delivery driver in this service.
 */
@FeignClient(
    name = "employee-management",
    path = "/api",
    configuration = FeignClientAuthConfig.class)
public interface EmployeeServiceClient {

  @GetMapping("/employees/{id}")
  EmployeeResponse getEmployeeById(@PathVariable("id") Long employeeId);

  // TODO: implement the endpoint the employee's microservice
  @GetMapping("/employees/drivers")
  List<EmployeeResponse> getDrivers();

  @GetMapping("/iam/users/{id}")
  IdentityUserResponse getById(@PathVariable("id") String id);
}
