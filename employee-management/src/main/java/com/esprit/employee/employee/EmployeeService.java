package com.esprit.employee.employee;

import com.esprit.employee.employee.dto.CreateEmployeeRequest;
import com.esprit.employee.employee.dto.UpdateEmployeeRequest;
import com.esprit.employee.exception.ApplicationException;
import com.esprit.employee.exception.ErrorCode;
import com.esprit.employee.iam.model.IdentityUser;
import com.esprit.employee.iam.model.KeycloakRequiredAction;
import com.esprit.employee.iam.model.RoleType;
import com.esprit.employee.iam.service.IdentityGateway;
import com.esprit.employee.messaging.ScheduleEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final IdentityGateway identityGateway;
    private final EmployeeRepository employeeRepository;
    private final ScheduleEventPublisher scheduleEventPublisher;

    @Transactional(readOnly = true)
    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return employeeRepository
                .findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EMPLOYEE_NOT_FOUND,
                        "Employee not found: " + id));
    }

    /**
     * Creates the account in Keycloak first (with role + "set password" / "verify email"
     * actions), persists only the needed info locally with the Keycloak user id. If the local
     * save fails, the Keycloak user is removed to avoid an orphaned identity.
     */
    public Employee create(CreateEmployeeRequest request) {
        if (request.getRole() == RoleType.MANAGER && request.getRestaurantId() == null) {
            throw new ApplicationException(ErrorCode.RESTAURANT_ID_REQUIRED,
                    "restaurantId is required when creating a MANAGER account");
        }
        if (employeeRepository.existsByEmail(request.getEmail())
                || Boolean.TRUE.equals(identityGateway.existsByEmail(request.getEmail()))) {
            throw new ApplicationException(ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email already in use: " + request.getEmail());
        }

        IdentityUser identityUser = IdentityUser.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .emailVerified(false)
                .roles(List.of(request.getRole()))
                .build();

        // sendEmail=false for now: dev Keycloak has no SMTP, and executeActionsEmail would throw.
        // The required actions are still attached to the user; flip to true once SMTP is configured.
        IdentityUser created = identityGateway.create(
                identityUser,
                List.of(KeycloakRequiredAction.UPDATE_PASSWORD, KeycloakRequiredAction.VERIFY_EMAIL),
                true);

        try {
            Employee employee = Employee.builder()
                    .keycloakUserId(created.getId())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .role(request.getRole())
                    .restaurantId(request.getRestaurantId())
                    .status(EmployeeStatus.ACTIVE)
                    .position(request.getPosition())
                    .contractType(request.getContractType())
                    .build();
            return employeeRepository.save(employee);
        } catch (RuntimeException e) {
            log.error("Local persistence failed after Keycloak user {} was created; rolling back the identity",
                    created.getId(), e);
            identityGateway.deleteById(created.getId());
            throw e;
        }
    }


    public Employee update(Long id, UpdateEmployeeRequest request) {
        Employee employee = getById(id);

        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getContractType() != null) {
            employee.setContractType(request.getContractType());
        }

        identityGateway.update(IdentityUser.builder()
                .id(employee.getKeycloakUserId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build());

        return employeeRepository.save(employee);
    }

    public void delete(Long id) {
        Employee employee = getById(id);
        identityGateway.deleteById(employee.getKeycloakUserId());
        employeeRepository.delete(employee);
    }

    public Employee enable(Long id) {
        Employee employee = getById(id);
        identityGateway.enableUser(employee.getKeycloakUserId());
        employee.setStatus(EmployeeStatus.ACTIVE);
        Employee saved = employeeRepository.save(employee);
        long activeCount = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        scheduleEventPublisher.publishScheduleUpdated(LocalDate.now(), (int) activeCount, employee.getKeycloakUserId());
        return saved;
    }

    public Employee disable(Long id) {
        Employee employee = getById(id);
        identityGateway.disableUser(employee.getKeycloakUserId());
        employee.setStatus(EmployeeStatus.INACTIVE);
        Employee saved = employeeRepository.save(employee);
        long activeCount = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        scheduleEventPublisher.publishScheduleUpdated(LocalDate.now(), (int) activeCount, employee.getKeycloakUserId());
        return saved;
    }
}
