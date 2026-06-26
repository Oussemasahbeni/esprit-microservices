package com.esprit.employee.employee;

import com.esprit.employee.exception.ApplicationException;
import com.esprit.employee.iam.service.IdentityGateway;
import com.esprit.employee.messaging.ScheduleEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceMessagingTest {

    @Mock
    private IdentityGateway identityGateway;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ScheduleEventPublisher scheduleEventPublisher;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void enable_publishesScheduleUpdatedWithCurrentActiveCount() {
        Employee employee = activeEmployee(1L, "keycloak-abc");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenReturn(employee);
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(5L);

        employeeService.enable(1L);

        verify(scheduleEventPublisher).publishScheduleUpdated(
                eq(LocalDate.now()), eq(5), eq("keycloak-abc")
        );
    }

    @Test
    void disable_publishesScheduleUpdatedWithReducedActiveCount() {
        Employee employee = activeEmployee(2L, "keycloak-xyz");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenReturn(employee);
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(3L);

        employeeService.disable(2L);

        verify(scheduleEventPublisher).publishScheduleUpdated(
                eq(LocalDate.now()), eq(3), eq("keycloak-xyz")
        );
    }

    @Test
    void enable_whenEmployeeNotFound_doesNotPublish() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.enable(99L))
                .isInstanceOf(ApplicationException.class);

        verify(scheduleEventPublisher, never()).publishScheduleUpdated(any(), any(), any());
    }

    @Test
    void disable_setsStatusToInactiveBeforePublishing() {
        Employee employee = activeEmployee(3L, "keycloak-def");
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(2L);

        Employee result = employeeService.disable(3L);

        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        verify(scheduleEventPublisher).publishScheduleUpdated(any(), eq(2), any());
    }

    private Employee activeEmployee(Long id, String keycloakUserId) {
        return Employee.builder()
                .id(id)
                .keycloakUserId(keycloakUserId)
                .firstName("Test")
                .lastName("Employee")
                .email("test@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
    }
}
