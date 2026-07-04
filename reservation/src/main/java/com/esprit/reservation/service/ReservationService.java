package com.esprit.reservation.service;

import com.esprit.reservation.client.EmployeeManagementClient;
import com.esprit.reservation.client.MenuManagementClient;
import com.esprit.reservation.client.MenuSnapshot;
import com.esprit.reservation.client.StaffAvailabilityResponse;
import com.esprit.reservation.domain.*;
import com.esprit.reservation.dto.PreOrderItemRequest;
import com.esprit.reservation.dto.ReservationResponse;
import com.esprit.reservation.dto.WaitlistEntryResponse;
import com.esprit.reservation.entity.*;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.WaitlistMapper;
import com.esprit.reservation.messaging.ReservationEventPublisher;
import com.esprit.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final WaitlistRepository waitlistRepository;
    private final AvailabilityService availabilityService;
    private final ReservationMapper reservationMapper;
    private final WaitlistMapper waitlistMapper;
    private final EmployeeManagementClient employeeManagementClient;
    private final MenuManagementClient menuManagementClient;
    private final ReservationEventPublisher eventPublisher;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date);
    }

    public List<Reservation> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findByReservationDateBetween(startDate, endDate);
    }

    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    public Optional<Reservation> getReservationByCode(ReservationCode code) {
        return reservationRepository.findByReservationCode(code);
    }

    public List<Reservation> getCustomerReservations(EmailAddress email) {
        return reservationRepository.findByCustomerEmail(email);
    }

    public List<Reservation> getCustomerReservationsByKeycloakId(String keycloakUserId) {
        return reservationRepository.findByKeycloakUserId(keycloakUserId);
    }

    @Transactional
    public BookingResult createReservation(
            String fullName, EmailAddress email, PhoneNumber phone, String keycloakUserId,
            LocalDate date, LocalTime startTime, GuestsCount guestsCount, String specialRequests,
            List<PreOrderItemRequest> preOrderItemRequests) {

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reservation date must be in the future.");
        }

        List<ReservationPreOrderItem> preOrderItems = resolvePreOrderItems(preOrderItemRequests);

        // --- Staff availability check (sync via FeignClient) ---
        StaffAvailabilityResponse staffCheck = safeCheckStaffAvailability(LocalDateTime.of(date, startTime));
        boolean staffWarning = !staffCheck.sufficient();
        if (staffWarning) {
            log.warn("Insufficient staff ({}) for date {} at {} — reservation confirmed but manager should review",
                    staffCheck.availableStaff(), date, startTime);
        }

        // Default duration is 2 hours
        LocalTime endTime = startTime.plusHours(2);

        // Find available tables
        List<RestaurantTable> availableTables = availabilityService
                .findAvailableTables(date, startTime, endTime, guestsCount);

        if (!availableTables.isEmpty()) {
            // Find the table that fits the guests count best (smallest capacity that fits)
            RestaurantTable assignedTable = availableTables.stream()
                    .min((t1, t2) -> Integer.compare(t1.getCapacity(), t2.getCapacity()))
                    .get();

            String generatedCodeStr = "RES-" + date.toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ReservationCode code = ReservationCode.of(generatedCodeStr);

            Reservation reservation = Reservation.builder()
                    .reservationCode(code)
                    .keycloakUserId(keycloakUserId)
                    .customerName(fullName)
                    .customerEmail(email)
                    .customerPhone(phone)
                    .table(assignedTable)
                    .reservationDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .guestsCount(guestsCount)
                    .status(ReservationStatus.CONFIRMED)
                    .specialRequests(specialRequests)
                    .build();

            // Record initial history
            reservation.getStatusHistory().add(
                ReservationStatusHistory.builder()
                    .reservation(reservation)
                    .oldStatus(null)
                    .newStatus(ReservationStatus.CONFIRMED)
                    .changedBy("SYSTEM")
                    .reason("Initial system confirmation")
                    .build()
            );

            preOrderItems.forEach(item -> item.setReservation(reservation));
            reservation.getPreOrderItems().addAll(preOrderItems);

            Reservation saved = reservationRepository.save(reservation);
            if (date.equals(LocalDate.now())) {
                assignedTable.setStatus(TableStatus.RESERVED);
                tableRepository.save(assignedTable);
            }
            eventPublisher.publishReservationConfirmed(saved);

            return BookingResult.builder()
                    .isSuccess(true)
                    .staffWarning(staffWarning)
                    .reservation(reservationMapper.toResponse(saved))
                    .message(staffWarning
                            ? "Reservation confirmed. Warning: insufficient staff scheduled for this time slot."
                            : "Reservation confirmed successfully.")
                    .build();
        } else {
            // No tables available, add to Waitlist
            WaitlistEntry waitlistEntry = WaitlistEntry.builder()
                    .keycloakUserId(keycloakUserId)
                    .customerName(fullName)
                    .customerEmail(email)
                    .customerPhone(phone)
                    .requestedDate(date)
                    .requestedTime(startTime)
                    .guestsCount(guestsCount)
                    .status(WaitlistStatus.WAITING)
                    .notes(specialRequests)
                    .priority(1)
                    .build();

            WaitlistEntry savedWaitlist = waitlistRepository.save(waitlistEntry);
            eventPublisher.publishReservationWaitlisted(savedWaitlist);

            return BookingResult.builder()
                    .isSuccess(false)
                    .staffWarning(staffWarning)
                    .message("No table available. Customer added to waiting list.")
                    .waitlistEntry(waitlistMapper.toResponse(savedWaitlist))
                    .build();
        }
    }

    private List<ReservationPreOrderItem> resolvePreOrderItems(List<PreOrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        MenuSnapshot menu;
        try {
            menu = menuManagementClient.getMenu();
        } catch (Exception e) {
            throw new IllegalStateException("Menu service is unavailable, cannot validate pre-order items.", e);
        }

        List<ReservationPreOrderItem> items = new ArrayList<>();
        for (PreOrderItemRequest request : requests) {
            MenuSnapshot.Dish dish = menu.dishes().stream()
                    .filter(d -> d.id().equals(request.dishId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Dish " + request.dishId() + " is not on the current menu."));
            if (!dish.available()) {
                throw new IllegalArgumentException(
                        "Dish '" + dish.name() + "' is currently unavailable and cannot be pre-ordered.");
            }
            items.add(ReservationPreOrderItem.builder()
                    .menuDishId(dish.id())
                    .dishName(dish.name())
                    .unitPrice(dish.price())
                    .quantity(request.quantity())
                    .stillAvailable(true)
                    .build());
        }
        return items;
    }

    private StaffAvailabilityResponse safeCheckStaffAvailability(LocalDateTime dateTime) {
        try {
            return employeeManagementClient.checkStaffAvailability(dateTime);
        } catch (Exception e) {
            log.warn("Employee management service unavailable — assuming sufficient staff: {}", e.getMessage());
            return new StaffAvailabilityResponse(dateTime, -1, true);
        }
    }

    @Transactional
    public Reservation cancelReservation(Long id, String reason, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.cancel(reason, changedBy);
        Reservation saved = reservationRepository.save(reservation);
        releaseTable(reservation);
        eventPublisher.publishReservationCancelled(saved);

        // Process waiting list for the released table
        processWaitingList(reservation.getReservationDate());

        return saved;
    }

    @Transactional
    public Reservation seatReservation(Long id, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.seat(changedBy);
        Reservation saved = reservationRepository.save(reservation);
        if (reservation.getTable() != null) {
            RestaurantTable table = reservation.getTable();
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }
        return saved;
    }

    @Transactional
    public Reservation completeReservation(Long id, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.complete(changedBy);
        Reservation saved = reservationRepository.save(reservation);
        releaseTable(reservation);

        // released table could accommodate waitlisted customers
        processWaitingList(reservation.getReservationDate());

        return saved;
    }

    @Transactional
    public Reservation markNoShow(Long id, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.markNoShow(changedBy);
        Reservation saved = reservationRepository.save(reservation);
        releaseTable(reservation);

        // released table could accommodate waitlisted customers
        processWaitingList(reservation.getReservationDate());

        return saved;
    }

    /**
     * Frees a reservation's table back to AVAILABLE — unless another still-active
     * (CONFIRMED/SEATED) reservation is also using it, e.g. a same-day back-to-back booking.
     */
    private void releaseTable(Reservation reservation) {
        RestaurantTable table = reservation.getTable();
        if (table == null) {
            return;
        }
        releaseTable(reservation.getId(), table, reservation.getReservationDate(),
                reservation.getStartTime(), reservation.getEndTime());
    }

    private void releaseTable(Long excludeReservationId, RestaurantTable table,
                               LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (table.getStatus() == TableStatus.OUT_OF_SERVICE) {
            return;
        }
        List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForTable(
                table.getId(), date, startTime, endTime
        );
        boolean stillActive = overlaps.stream().anyMatch(r -> !r.getId().equals(excludeReservationId));
        if (!stillActive) {
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        }
    }

    @Transactional
    public Reservation assignTable(Long id, Long tableId, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found with ID: " + tableId));

        if (table.getCapacity() < reservation.getGuestsCount().value()) {
            throw new IllegalArgumentException("Table capacity is smaller than reservation guests count.");
        }

        // Check if table is free during that time
        List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForTable(
                tableId, reservation.getReservationDate(), reservation.getStartTime(), reservation.getEndTime()
        );

        // Exclude the current reservation from overlapping check
        boolean hasOverlap = overlaps.stream().anyMatch(r -> !r.getId().equals(id));
        if (hasOverlap) {
            throw new IllegalArgumentException("Table is already reserved for an overlapping time slot.");
        }

        RestaurantTable previousTable = reservation.getTable();
        reservation.assignTable(table, changedBy);
        Reservation saved = reservationRepository.save(reservation);

        if (reservation.getStatus() == ReservationStatus.SEATED) {
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        } else if (reservation.getReservationDate().equals(LocalDate.now())) {
            table.setStatus(TableStatus.RESERVED);
            tableRepository.save(table);
        }
        if (previousTable != null && !previousTable.getId().equals(table.getId())) {
            releaseTable(id, previousTable, reservation.getReservationDate(),
                    reservation.getStartTime(), reservation.getEndTime());
        }
        return saved;
    }

    @Transactional
    public void processWaitingList(LocalDate date) {
        List<WaitlistEntry> waitlist = waitlistRepository
                .findByRequestedDateAndStatusOrderByPriorityDescCreatedAtAsc(date, WaitlistStatus.WAITING);

        for (WaitlistEntry entry : waitlist) {
            tryPromoteEntry(entry);
        }
    }

    /**
     * Attempts to promote a specific waitlist entry right now. Unlike {@link #processWaitingList},
     * this reports back whether a table was actually found — the caller (a manager clicking
     * "Promote") needs to know if their click did anything, not just get a silent no-op.
     */
    @Transactional
    public boolean promoteWaitlistEntryNow(WaitlistEntry entry) {
        return tryPromoteEntry(entry);
    }

    private boolean tryPromoteEntry(WaitlistEntry entry) {
        LocalDate date = entry.getRequestedDate();
        LocalTime start = entry.getRequestedTime();
        LocalTime end = start.plusHours(2);

        List<RestaurantTable> availableTables = availabilityService
                .findAvailableTables(date, start, end, entry.getGuestsCount());

        if (availableTables.isEmpty()) {
            return false;
        }

        RestaurantTable assignedTable = availableTables.stream()
                .min((t1, t2) -> Integer.compare(t1.getCapacity(), t2.getCapacity()))
                .get();

        String generatedCodeStr = "RES-" + date.toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReservationCode code = ReservationCode.of(generatedCodeStr);

        Reservation reservation = Reservation.builder()
                .reservationCode(code)
                .keycloakUserId(entry.getKeycloakUserId())
                .customerName(entry.getCustomerName())
                .customerEmail(entry.getCustomerEmail())
                .customerPhone(entry.getCustomerPhone())
                .table(assignedTable)
                .reservationDate(date)
                .startTime(start)
                .endTime(end)
                .guestsCount(entry.getGuestsCount())
                .status(ReservationStatus.CONFIRMED)
                .specialRequests(entry.getNotes())
                .build();

        // Record history
        reservation.getStatusHistory().add(
            ReservationStatusHistory.builder()
                .reservation(reservation)
                .oldStatus(null)
                .newStatus(ReservationStatus.CONFIRMED)
                .changedBy("SYSTEM")
                .reason("Promoted from waitlist")
                .build()
        );

        reservationRepository.save(reservation);
        if (date.equals(LocalDate.now())) {
            assignedTable.setStatus(TableStatus.RESERVED);
            tableRepository.save(assignedTable);
        }

        entry.setStatus(WaitlistStatus.PROMOTED);
        waitlistRepository.save(entry);
        return true;
    }

    // Helper classes for result payload wrapping
    @lombok.Value
    @lombok.Builder
    public static class BookingResult {
        boolean isSuccess;
        boolean staffWarning;
        ReservationResponse reservation;
        WaitlistEntryResponse waitlistEntry;
        String message;
    }
}
