package com.esprit.employee.employee.dto;

import com.esprit.employee.employee.ContractType;
import com.esprit.employee.employee.EmployeeStatus;
import com.esprit.employee.iam.model.RoleType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String keycloakUserId,
        String firstName,
        String lastName,
        String email,
        String phone,
        RoleType role,
        Long restaurantId,
        EmployeeStatus status,
        String position,
        ContractType contractType,
        LocalDate hireDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
