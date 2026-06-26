package com.esprit.employee.messaging;

import com.esprit.employee.employee.Employee;
import com.esprit.employee.staff.StaffAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationEventConsumerTest {

    @Mock
    private StaffAssignmentService staffAssignmentService;

    @InjectMocks
    private ReservationEventConsumer consumer;

    private ReservationConfirmedEvent event() {
        return new ReservationConfirmedEvent(
                "evt-1", "reservation.confirmed", 15L, "RES-20260525-ABCD1234",
                "ahmed@gmail.com", "+21622111222",
                LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), 4, "T04",
                LocalDateTime.of(2026, 5, 23, 18, 30));
    }

    @Test
    void onReservationConfirmed_delegatesAssignmentWithEventData() {
        Employee assigned = new Employee();
        assigned.setId(7L);
        when(staffAssignmentService.assignStaff("RES-20260525-ABCD1234",
                LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), 4))
                .thenReturn(Optional.of(assigned));

        consumer.onReservationConfirmed(event());

        verify(staffAssignmentService).assignStaff(
                "RES-20260525-ABCD1234", LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), 4);
    }

    @Test
    void onReservationConfirmed_whenNoStaffAvailable_stillCompletesWithoutThrowing() {
        when(staffAssignmentService.assignStaff(
                "RES-20260525-ABCD1234", LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), 4))
                .thenReturn(Optional.empty());

        consumer.onReservationConfirmed(event()); // should not throw — shortage is logged

        verify(staffAssignmentService).assignStaff(
                "RES-20260525-ABCD1234", LocalDate.of(2026, 5, 25), LocalTime.of(20, 0), 4);
    }
}
