package com.esprit.employee.employee.dto;

import com.esprit.employee.employee.ContractType;
import com.esprit.employee.employee.EmployeeStatus;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {

    private String firstName;

    private String lastName;

    private String phone;

    private EmployeeStatus status;

    private String position;

    private ContractType contractType;
}
