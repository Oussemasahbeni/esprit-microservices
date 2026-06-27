package com.esprit.employee.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAvailabilityService {

    /**
     * Minimum employees that must still be free for the requested slot for it to count as staffed.
     * A Friday 20:00 where every active waiter is already at capacity → availableStaff=0 →
     * sufficient=false → reservation service warns the manager.
     */
    private static final int MIN_STAFF_PER_SHIFT = 1;

    private final StaffAssignmentService staffAssignmentService;

    public StaffAvailabilityResponse checkAvailability(LocalDateTime dateTime) {
        int available = staffAssignmentService.countAvailableStaff(dateTime.toLocalDate(), dateTime.toLocalTime());
        boolean sufficient = available >= MIN_STAFF_PER_SHIFT;
        return new StaffAvailabilityResponse(dateTime, available, sufficient);
    }
}
