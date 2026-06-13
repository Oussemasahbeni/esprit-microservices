package com.esprit.employee.employee.dto;

import com.esprit.employee.employee.ContractType;
import com.esprit.employee.iam.model.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    @NotNull(message = "Role is required")
    private RoleType role;

    /**
     * Required when role = MANAGER (validated in the service).
     */
    private Long restaurantId;

    private String position;

    private ContractType contractType;
}
