package com.esprit.employee.staff;

import com.esprit.employee.employee.Employee;
import com.esprit.employee.employee.EmployeeRepository;
import com.esprit.employee.employee.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAvailabilityServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private StaffAvailabilityService staffAvailabilityService;

    private final LocalDate date = LocalDate.of(2026, 5, 25);
    private final LocalTime time = LocalTime.of(20, 0);

    @Test
    void checkAvailability_withActiveStaff_returnsSufficient() {
        when(employeeRepository.findAll()).thenReturn(List.of(
                activeEmployee("Alice"),
                activeEmployee("Bob"),
                activeEmployee("Carlos")
        ));

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(date, time);

        assertThat(response.availableStaff()).isEqualTo(3);
        assertThat(response.sufficient()).isTrue();
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.time()).isEqualTo(time);
    }

    @Test
    void checkAvailability_withNoActiveStaff_returnsInsufficient() {
        // Friday night: 30 reservations but 0 active employees → manager must be warned
        when(employeeRepository.findAll()).thenReturn(List.of(
                inactiveEmployee("Dave"),
                inactiveEmployee("Eve")
        ));

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(date, time);

        assertThat(response.availableStaff()).isEqualTo(0);
        assertThat(response.sufficient()).isFalse();
    }

    @Test
    void checkAvailability_withMixedStatus_countsOnlyActive() {
        when(employeeRepository.findAll()).thenReturn(List.of(
                activeEmployee("Alice"),
                inactiveEmployee("Dave"),
                suspendedEmployee("Frank")
        ));

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(date, time);

        assertThat(response.availableStaff()).isEqualTo(1);
        assertThat(response.sufficient()).isTrue();
    }

    @Test
    void checkAvailability_withEmptyDatabase_returnsInsufficient() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(date, time);

        assertThat(response.availableStaff()).isEqualTo(0);
        assertThat(response.sufficient()).isFalse();
    }

    private Employee activeEmployee(String name) {
        return Employee.builder().firstName(name).status(EmployeeStatus.ACTIVE).build();
    }

    private Employee inactiveEmployee(String name) {
        return Employee.builder().firstName(name).status(EmployeeStatus.INACTIVE).build();
    }

    private Employee suspendedEmployee(String name) {
        return Employee.builder().firstName(name).status(EmployeeStatus.SUSPENDED).build();
    }
}
