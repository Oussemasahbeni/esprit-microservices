package com.esprit.employee.employee;

import com.esprit.employee.employee.dto.CreateEmployeeRequest;
import com.esprit.employee.employee.dto.EmployeeResponse;
import com.esprit.employee.employee.dto.UpdateEmployeeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeMapper.toResponse(employee));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public List<EmployeeResponse> getAll() {
        return employeeMapper.toResponseList(employeeService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public EmployeeResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeMapper.toResponse(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('admin')")
    public EmployeeResponse enable(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.enable(id));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('admin')")
    public EmployeeResponse disable(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.disable(id));
    }
}
