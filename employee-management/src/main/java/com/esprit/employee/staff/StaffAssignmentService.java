package com.esprit.employee.staff;

import com.esprit.employee.employee.Employee;
import com.esprit.employee.employee.EmployeeRepository;
import com.esprit.employee.employee.EmployeeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * Owns the real, time-aware notion of staff capacity.
 * <p>
 * An employee is <em>assignable</em> to a slot when they are {@link EmployeeStatus#ACTIVE} and
 * already hold fewer than {@link #MAX_RESERVATIONS_PER_EMPLOYEE} assignments overlapping that slot
 * — a single waiter can realistically handle several tables at once, but not unlimited. This is
 * what replaces the old "count every ACTIVE employee regardless of date/time" logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAssignmentService {

    /** A reservation occupies its table (and therefore needs serving) for this long. */
    public static final int SLOT_DURATION_HOURS = 2;

    /** How many overlapping reservations one active employee can serve at the same time. */
    public static final int MAX_RESERVATIONS_PER_EMPLOYEE = 5;

    private final EmployeeRepository employeeRepository;
    private final StaffAssignmentRepository assignmentRepository;

    /**
     * Counts active employees that still have spare capacity for the slot starting at
     * {@code start} on {@code date} (the slot runs {@link #SLOT_DURATION_HOURS} hours).
     */
    @Transactional(readOnly = true)
    public int countAvailableStaff(LocalDate date, LocalTime start) {
        LocalTime end = start.plusHours(SLOT_DURATION_HOURS);
        return (int) employeeRepository.findByStatus(EmployeeStatus.ACTIVE).stream()
                .filter(e -> currentLoad(e, date, start, end) < MAX_RESERVATIONS_PER_EMPLOYEE)
                .count();
    }

    /**
     * Assigns the least-loaded available employee to a confirmed reservation and persists the link.
     * Idempotent: a reservation code already assigned is returned as-is without creating a duplicate.
     *
     * @return the assigned employee, or empty if every active employee is at capacity for the slot.
     */
    @Transactional
    public Optional<Employee> assignStaff(String reservationCode, LocalDate date, LocalTime start, int guestsCount) {
        if (assignmentRepository.existsByReservationCode(reservationCode)) {
            log.info("Reservation {} already has a staff assignment — skipping (idempotent)", reservationCode);
            return Optional.empty();
        }

        LocalTime end = start.plusHours(SLOT_DURATION_HOURS);

        Optional<Employee> candidate = employeeRepository.findByStatus(EmployeeStatus.ACTIVE).stream()
                .filter(e -> currentLoad(e, date, start, end) < MAX_RESERVATIONS_PER_EMPLOYEE)
                .min(Comparator.comparingLong(e -> currentLoad(e, date, start, end)));

        if (candidate.isEmpty()) {
            log.warn("No available staff to assign for reservation {} on {} at {} — manager should review",
                    reservationCode, date, start);
            return Optional.empty();
        }

        Employee employee = candidate.get();
        StaffAssignment assignment = StaffAssignment.builder()
                .employee(employee)
                .reservationCode(reservationCode)
                .reservationDate(date)
                .startTime(start)
                .endTime(end)
                .guestsCount(guestsCount)
                .build();
        assignmentRepository.save(assignment);

        log.info("Assigned employee {} ({}) to reservation {} on {} at {}",
                employee.getId(), employee.getEmail(), reservationCode, date, start);
        return candidate;
    }

    private long currentLoad(Employee employee, LocalDate date, LocalTime start, LocalTime end) {
        return assignmentRepository
                .countByEmployeeIdAndReservationDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        employee.getId(), date, end, start);
    }
}
