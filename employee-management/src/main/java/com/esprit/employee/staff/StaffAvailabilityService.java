package com.esprit.employee.staff;

import com.esprit.employee.employee.EmployeeRepository;
import com.esprit.employee.employee.EmployeeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAvailabilityService {

    /**
     * Minimum active employees required to run a shift.
     * A Friday with 30 reservations but 0 active staff → sufficient=false → manager warned.
     */
    private static final int MIN_STAFF_PER_SHIFT = 1;

    private final EmployeeRepository employeeRepository;

    public StaffAvailabilityResponse checkAvailability(LocalDate date, LocalTime time) {
        long count = employeeRepository.findAll()
                .stream()
                .filter(e -> EmployeeStatus.ACTIVE == e.getStatus())
                .count();

        boolean sufficient = count >= MIN_STAFF_PER_SHIFT;
        return new StaffAvailabilityResponse(date, time, (int) count, sufficient);
    }
}
