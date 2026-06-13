package com.esprit.employee.employee;

import com.esprit.employee.iam.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByKeycloakUserId(String keycloakUserId);

    List<Employee> findByRestaurantId(Long restaurantId);

    List<Employee> findByRole(RoleType role);

    boolean existsByEmail(String email);
}
