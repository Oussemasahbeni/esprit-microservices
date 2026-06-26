package com.esprit.employee.messaging;

import com.esprit.employee.employee.Employee;
import com.esprit.employee.staff.StaffAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventConsumer {

    private final StaffAssignmentService staffAssignmentService;

    /**
     * On every confirmed reservation we try to assign a real, free employee to serve it.
     * This is the async (RabbitMQ) leg that keeps the employee database in sync with bookings:
     * a successful assignment reduces that employee's remaining capacity for the slot; if no one
     * is free the shortage is logged so the manager can react.
     */
    @RabbitListener(queues = RabbitMqConfig.RESERVATION_CONFIRMED_QUEUE)
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        log.info("Reservation confirmed: {} on {} at {} for {} guests — assigning staff",
                event.reservationCode(), event.reservationDate(), event.startTime(), event.guestsCount());

        Optional<Employee> assigned = staffAssignmentService.assignStaff(
                event.reservationCode(),
                event.reservationDate(),
                event.startTime(),
                event.guestsCount());

        if (assigned.isEmpty()) {
            log.warn("STAFF ALERT: reservation {} on {} at {} could not be staffed — all employees at capacity",
                    event.reservationCode(), event.reservationDate(), event.startTime());
        }
    }
}
