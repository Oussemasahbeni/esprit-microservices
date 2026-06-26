package com.esprit.employee.staff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAvailabilityServiceTest {

    @Mock
    private StaffAssignmentService staffAssignmentService;

    @InjectMocks
    private StaffAvailabilityService staffAvailabilityService;

    private final LocalDateTime fridayNight = LocalDateTime.of(2026, 5, 25, 20, 0);

    @Test
    void checkAvailability_withFreeStaff_returnsSufficient() {
        when(staffAssignmentService.countAvailableStaff(LocalDate.of(2026, 5, 25), LocalTime.of(20, 0)))
                .thenReturn(3);

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(fridayNight);

        assertThat(response.availableStaff()).isEqualTo(3);
        assertThat(response.sufficient()).isTrue();
        assertThat(response.dateTime()).isEqualTo(fridayNight);
    }

    @Test
    void checkAvailability_whenEveryoneAtCapacity_returnsInsufficient() {
        when(staffAssignmentService.countAvailableStaff(LocalDate.of(2026, 5, 25), LocalTime.of(20, 0)))
                .thenReturn(0);

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(fridayNight);

        assertThat(response.availableStaff()).isZero();
        assertThat(response.sufficient()).isFalse();
    }

    @Test
    void checkAvailability_withOneFreeEmployee_returnsSufficient() {
        when(staffAssignmentService.countAvailableStaff(LocalDate.of(2026, 5, 25), LocalTime.of(20, 0)))
                .thenReturn(1);

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(fridayNight);

        assertThat(response.availableStaff()).isEqualTo(1);
        assertThat(response.sufficient()).isTrue();
    }

    @Test
    void checkAvailability_passesDateAndTimeThroughAndPreservesDateTime() {
        LocalDateTime specificSlot = LocalDateTime.of(2026, 12, 31, 23, 0);
        when(staffAssignmentService.countAvailableStaff(LocalDate.of(2026, 12, 31), LocalTime.of(23, 0)))
                .thenReturn(2);

        StaffAvailabilityResponse response = staffAvailabilityService.checkAvailability(specificSlot);

        assertThat(response.dateTime()).isEqualTo(specificSlot);
        assertThat(response.availableStaff()).isEqualTo(2);
    }
}
