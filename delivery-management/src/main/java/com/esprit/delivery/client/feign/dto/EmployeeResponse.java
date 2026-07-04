package com.esprit.delivery.client.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subset of the employee representation exposed by MS1 (Employee Management
 * Service), as returned by {@code GET /employees/{id}}. Used to validate that
 * an employee exists (and holds the DRIVER role) before registering them as
 * a {@code Driver} in this service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
}
