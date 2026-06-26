package com.esprit.employee.staff;

import com.esprit.employee.employee.Employee;
import com.esprit.employee.employee.EmployeeRepository;
import com.esprit.employee.employee.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.esprit.employee.staff.StaffAssignmentService.MAX_RESERVATIONS_PER_EMPLOYEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAssignmentServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private StaffAssignmentRepository assignmentRepository;

    @InjectMocks
    private StaffAssignmentService service;

    private final LocalDate date = LocalDate.of(2026, 5, 25);
    private final LocalTime start = LocalTime.of(20, 0);
    private final LocalTime end = LocalTime.of(22, 0);

    private Employee employee(long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setEmail("emp" + id + "@esprit.tn");
        e.setStatus(EmployeeStatus.ACTIVE);
        return e;
    }

    private void stubLoad(long employeeId, long load) {
        when(assignmentRepository.countByEmployeeIdAndReservationDateAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(employeeId), eq(date), eq(end), eq(start))).thenReturn(load);
    }

    @Test
    void countAvailableStaff_excludesEmployeesAtCapacity() {
        when(employeeRepository.findByStatus(EmployeeStatus.ACTIVE))
                .thenReturn(List.of(employee(1), employee(2), employee(3)));
        stubLoad(1, 0);                                  // free
        stubLoad(2, MAX_RESERVATIONS_PER_EMPLOYEE);      // full
        stubLoad(3, MAX_RESERVATIONS_PER_EMPLOYEE - 1);  // one slot left

        assertThat(service.countAvailableStaff(date, start)).isEqualTo(2);
    }

    @Test
    void countAvailableStaff_withNoActiveEmployees_isZero() {
        when(employeeRepository.findByStatus(EmployeeStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.countAvailableStaff(date, start)).isZero();
    }

    @Test
    void assignStaff_picksLeastLoadedAvailableEmployeeAndPersists() {
        when(assignmentRepository.existsByReservationCode("RES-1")).thenReturn(false);
        when(employeeRepository.findByStatus(EmployeeStatus.ACTIVE))
                .thenReturn(List.of(employee(1), employee(2)));
        stubLoad(1, 3);
        stubLoad(2, 1); // least loaded → should be chosen

        Optional<Employee> assigned = service.assignStaff("RES-1", date, start, 4);

        assertThat(assigned).isPresent();
        assertThat(assigned.get().getId()).isEqualTo(2L);

        ArgumentCaptor<StaffAssignment> captor = ArgumentCaptor.forClass(StaffAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        StaffAssignment saved = captor.getValue();
        assertThat(saved.getReservationCode()).isEqualTo("RES-1");
        assertThat(saved.getEmployee().getId()).isEqualTo(2L);
        assertThat(saved.getStartTime()).isEqualTo(start);
        assertThat(saved.getEndTime()).isEqualTo(end);
        assertThat(saved.getGuestsCount()).isEqualTo(4);
    }

    @Test
    void assignStaff_whenAllAtCapacity_returnsEmptyAndDoesNotPersist() {
        when(assignmentRepository.existsByReservationCode("RES-2")).thenReturn(false);
        when(employeeRepository.findByStatus(EmployeeStatus.ACTIVE))
                .thenReturn(List.of(employee(1), employee(2)));
        stubLoad(1, MAX_RESERVATIONS_PER_EMPLOYEE);
        stubLoad(2, MAX_RESERVATIONS_PER_EMPLOYEE);

        Optional<Employee> assigned = service.assignStaff("RES-2", date, start, 2);

        assertThat(assigned).isEmpty();
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignStaff_isIdempotentForAlreadyAssignedReservation() {
        when(assignmentRepository.existsByReservationCode("RES-DUP")).thenReturn(true);

        Optional<Employee> assigned = service.assignStaff("RES-DUP", date, start, 2);

        assertThat(assigned).isEmpty();
        verify(employeeRepository, never()).findByStatus(any());
        verify(assignmentRepository, never()).save(any());
        verify(assignmentRepository, never())
                .countByEmployeeIdAndReservationDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        anyLong(), any(), any(), any());
    }
}
