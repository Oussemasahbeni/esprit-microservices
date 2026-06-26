package com.esprit.employee.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    boolean existsByReservationCode(String reservationCode);

    /**
     * Number of assignments a given employee already holds that overlap the slot
     * [{@code start}, {@code end}) on {@code date}. Standard half-open overlap test:
     * an existing assignment overlaps when {@code existing.start < end} and {@code existing.end > start}.
     */
    long countByEmployeeIdAndReservationDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Long employeeId, LocalDate reservationDate, LocalTime end, LocalTime start);
}
