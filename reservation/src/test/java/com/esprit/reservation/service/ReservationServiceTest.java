package com.esprit.reservation.service;

import com.esprit.reservation.client.EmployeeManagementClient;
import com.esprit.reservation.client.StaffAvailabilityResponse;
import com.esprit.reservation.domain.*;
import com.esprit.reservation.dto.*;
import com.esprit.reservation.entity.*;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.WaitlistMapper;
import com.esprit.reservation.messaging.ReservationEventPublisher;
import com.esprit.reservation.repository.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RestaurantTableRepository tableRepository;
    @Mock
    private WaitlistRepository waitlistRepository;
    @Mock
    private AvailabilityService availabilityService;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private WaitlistMapper waitlistMapper;
    @Mock
    private EmployeeManagementClient employeeManagementClient;
    @Mock
    private ReservationEventPublisher eventPublisher;
    @Mock
    private com.esprit.reservation.client.MenuManagementClient menuManagementClient;

    @InjectMocks
    private ReservationService reservationService;

    private RestaurantTable table;
    private LocalDate futureDate;
    private LocalTime startTime;

    @BeforeEach
    void setUp() {
        table = RestaurantTable.builder()
                .id(1L)
                .tableNumber("T1")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .build();

        futureDate = LocalDate.now().plusDays(2);
        startTime = LocalTime.of(19, 0);

        // Default: employee service reports sufficient staff so existing tests are unaffected.
        // lenient() because testCancelReservation doesn't exercise this path at all.
        lenient().when(employeeManagementClient.checkStaffAvailability(any()))
                .thenReturn(new StaffAvailabilityResponse(futureDate.atTime(startTime), 3, true));
    }

    @Test
    void testCreateReservationSuccess() {
        when(availabilityService.findAvailableTables(any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), any(GuestsCount.class)))
                .thenReturn(List.of(table));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse reservationResponse = new ReservationResponse(
                1L,
                ReservationCode.of("RES-20260525-ABCDEF12"),
                new CustomerResponse(null, "some-keycloak-id", "John Doe", EmailAddress.of("john.doe@example.com"), PhoneNumber.of("1234567890")),
                new TableResponse(1L, null, "T1", 4, null, null, null, TableStatus.AVAILABLE, true),
                futureDate,
                startTime,
                startTime.plusHours(2),
                GuestsCount.of(2),
                ReservationStatus.CONFIRMED,
                "ONLINE",
                "Window seat",
                null,
                List.of(),
                List.of(),
                null,
                null
        );
        when(reservationMapper.toResponse(any(Reservation.class))).thenReturn(reservationResponse);

        ReservationService.BookingResult result = reservationService.createReservation(
                "John Doe",
                EmailAddress.of("john.doe@example.com"),
                PhoneNumber.of("1234567890"),
                "some-keycloak-id",
                futureDate,
                startTime,
                GuestsCount.of(2),
                "Window seat",
                null
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getReservation());
        assertEquals(ReservationStatus.CONFIRMED, result.getReservation().status());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void testCreateReservationWaitlistedWhenNoTable() {
        when(availabilityService.findAvailableTables(any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), any(GuestsCount.class)))
                .thenReturn(List.of());

        WaitlistEntry waitlistEntry = WaitlistEntry.builder()
                .id(1L)
                .keycloakUserId("some-keycloak-id")
                .customerName("John Doe")
                .customerEmail(EmailAddress.of("john.doe@example.com"))
                .customerPhone(PhoneNumber.of("1234567890"))
                .requestedDate(futureDate)
                .requestedTime(startTime)
                .guestsCount(GuestsCount.of(2))
                .status(WaitlistStatus.WAITING)
                .build();
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenReturn(waitlistEntry);

        WaitlistEntryResponse waitlistResponse = new WaitlistEntryResponse(
                1L,
                new CustomerResponse(null, "some-keycloak-id", "John Doe", EmailAddress.of("john.doe@example.com"), PhoneNumber.of("1234567890")),
                futureDate,
                startTime,
                GuestsCount.of(2),
                WaitlistStatus.WAITING,
                1,
                "Window seat",
                null
        );
        when(waitlistMapper.toResponse(any(WaitlistEntry.class))).thenReturn(waitlistResponse);

        ReservationService.BookingResult result = reservationService.createReservation(
                "John Doe",
                EmailAddress.of("john.doe@example.com"),
                PhoneNumber.of("1234567890"),
                "some-keycloak-id",
                futureDate,
                startTime,
                GuestsCount.of(2),
                "Window seat",
                null
        );

        assertFalse(result.isSuccess());
        assertNull(result.getReservation());
        assertNotNull(result.getWaitlistEntry());
        assertEquals(WaitlistStatus.WAITING, result.getWaitlistEntry().status());
        verify(waitlistRepository, times(1)).save(any(WaitlistEntry.class));
    }

    @Test
    void testCancelReservation() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationCode(ReservationCode.of("RES-20260525-ABCDEF12"))
                .keycloakUserId("some-keycloak-id")
                .customerName("John Doe")
                .customerEmail(EmailAddress.of("john.doe@example.com"))
                .customerPhone(PhoneNumber.of("1234567890"))
                .table(table)
                .reservationDate(futureDate)
                .startTime(startTime)
                .endTime(startTime.plusHours(2))
                .guestsCount(GuestsCount.of(2))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation cancelled = reservationService.cancelReservation(1L, "Changed plans", "CLIENT");

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Changed plans", cancelled.getCancellationReason());
        verify(reservationRepository, times(1)).save(reservation);
    }
}
