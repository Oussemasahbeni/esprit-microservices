package com.esprit.reservation.service;

import com.esprit.reservation.client.EmployeeManagementClient;
import com.esprit.reservation.client.StaffAvailabilityResponse;
import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.domain.PhoneNumber;
import com.esprit.reservation.domain.ReservationCode;
import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.ReservationStatus;
import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.entity.TableStatus;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.WaitlistMapper;
import com.esprit.reservation.messaging.ReservationEventPublisher;
import com.esprit.reservation.repository.ReservationRepository;
import com.esprit.reservation.repository.RestaurantTableRepository;
import com.esprit.reservation.repository.WaitlistRepository;
import com.esprit.reservation.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceStaffCheckTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RestaurantTableRepository tableRepository;
    @Mock private WaitlistRepository waitlistRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private ReservationMapper reservationMapper;
    @Mock private WaitlistMapper waitlistMapper;
    @Mock private EmployeeManagementClient employeeManagementClient;
    @Mock private ReservationEventPublisher eventPublisher;
    @Mock private com.esprit.reservation.client.MenuManagementClient menuManagementClient;

    @InjectMocks
    private ReservationService reservationService;

    private RestaurantTable table;
    private LocalDate futureDate;
    private LocalTime startTime;

    @BeforeEach
    void setUp() {
        table = RestaurantTable.builder()
                .id(1L).tableNumber("T1").capacity(4).status(TableStatus.AVAILABLE).build();
        futureDate = LocalDate.now().plusDays(2);
        startTime = LocalTime.of(19, 0);
    }

    @Test
    void createReservation_withSufficientStaff_confirmsWithNoWarning() {
        when(employeeManagementClient.checkStaffAvailability(any()))
                .thenReturn(new StaffAvailabilityResponse(futureDate.atTime(startTime), 3, true));
        when(availabilityService.findAvailableTables(any(), any(), any(), any()))
                .thenReturn(List.of(table));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(reservationMapper.toResponse(any())).thenReturn(null);

        ReservationService.BookingResult result = invokeCreate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isStaffWarning()).isFalse();
        assertThat(result.getMessage()).contains("confirmed successfully");
        verify(eventPublisher).publishReservationConfirmed(any());
    }

    @Test
    void createReservation_withInsufficientStaff_confirmsButRaisesWarning() {
        // 0 active staff on a fully booked Friday night → warning raised, but booking still goes through
        when(employeeManagementClient.checkStaffAvailability(any()))
                .thenReturn(new StaffAvailabilityResponse(futureDate.atTime(startTime), 0, false));
        when(availabilityService.findAvailableTables(any(), any(), any(), any()))
                .thenReturn(List.of(table));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(reservationMapper.toResponse(any())).thenReturn(null);

        ReservationService.BookingResult result = invokeCreate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isStaffWarning()).isTrue();
        assertThat(result.getMessage()).contains("Warning: insufficient staff");
        verify(eventPublisher).publishReservationConfirmed(any());
    }

    @Test
    void createReservation_whenEmployeeServiceDown_fallbackAllowsBookingWithoutWarning() {
        // Feign call throws → safeCheckStaffAvailability returns sufficient=true
        when(employeeManagementClient.checkStaffAvailability(any()))
                .thenThrow(new RuntimeException("employee-management service unavailable"));
        when(availabilityService.findAvailableTables(any(), any(), any(), any()))
                .thenReturn(List.of(table));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(reservationMapper.toResponse(any())).thenReturn(null);

        ReservationService.BookingResult result = invokeCreate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isStaffWarning()).isFalse();
        verify(eventPublisher).publishReservationConfirmed(any());
    }

    @Test
    void cancelReservation_publishesCancelledEvent() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationCode(ReservationCode.of("RES-20260525-ABCDEF12"))
                .keycloakUserId("kc-id")
                .customerName("John Doe")
                .customerEmail(EmailAddress.of("john@example.com"))
                .customerPhone(PhoneNumber.of("+21622111222"))
                .table(table)
                .reservationDate(futureDate)
                .startTime(startTime)
                .endTime(startTime.plusHours(2))
                .guestsCount(GuestsCount.of(2))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        reservationService.cancelReservation(1L, "Changed plans", "CLIENT");

        verify(eventPublisher).publishReservationCancelled(any());
        verify(eventPublisher, never()).publishReservationConfirmed(any());
    }

    private ReservationService.BookingResult invokeCreate() {
        return reservationService.createReservation(
                "John Doe",
                EmailAddress.of("john@example.com"),
                PhoneNumber.of("+21622111222"),
                "kc-id",
                futureDate,
                startTime,
                GuestsCount.of(2),
                null,
                null
        );
    }
}
