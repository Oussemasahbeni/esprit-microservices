package com.esprit.reservation.service;

import com.esprit.reservation.entity.*;
import com.esprit.reservation.domain.*;
import com.esprit.reservation.dto.ReservationResponse;
import com.esprit.reservation.dto.WaitlistEntryResponse;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.WaitlistMapper;
import com.esprit.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final WaitlistRepository waitlistRepository;
    private final AvailabilityService availabilityService;
    private final ReservationMapper reservationMapper;
    private final WaitlistMapper waitlistMapper;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date);
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
            LocalDate date, LocalTime startTime, GuestsCount guestsCount, String specialRequests) {

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reservation date must be in the future.");
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

            Reservation saved = reservationRepository.save(reservation);

            return BookingResult.builder()
                    .isSuccess(true)
                    .reservation(reservationMapper.toResponse(saved))
                    .message("Reservation confirmed successfully.")
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

            return BookingResult.builder()
                    .isSuccess(false)
                    .message("No table available. Customer added to waiting list.")
                    .waitlistEntry(waitlistMapper.toResponse(savedWaitlist))
                    .build();
        }
    }

    @Transactional
    public Reservation cancelReservation(Long id, String reason, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.cancel(reason, changedBy);
        Reservation saved = reservationRepository.save(reservation);

        // Process waiting list for the released table
        processWaitingList(reservation.getReservationDate());

        return saved;
    }

    @Transactional
    public Reservation seatReservation(Long id, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.seat(changedBy);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation completeReservation(Long id, String changedBy) {
        Reservation reservation = getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.complete(changedBy);
        Reservation saved = reservationRepository.save(reservation);

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

        // released table could accommodate waitlisted customers
        processWaitingList(reservation.getReservationDate());

        return saved;
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

        reservation.assignTable(table, changedBy);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void processWaitingList(LocalDate date) {
        List<WaitlistEntry> waitlist = waitlistRepository
                .findByRequestedDateAndStatusOrderByPriorityDescCreatedAtAsc(date, WaitlistStatus.WAITING);

        for (WaitlistEntry entry : waitlist) {
            LocalTime start = entry.getRequestedTime();
            LocalTime end = start.plusHours(2);

            List<RestaurantTable> availableTables = availabilityService
                    .findAvailableTables(date, start, end, entry.getGuestsCount());

            if (!availableTables.isEmpty()) {
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

                entry.setStatus(WaitlistStatus.PROMOTED);
                waitlistRepository.save(entry);
            }
        }
    }

    // Helper classes for result payload wrapping
    @lombok.Value
    @lombok.Builder
    public static class BookingResult {
        boolean isSuccess;
        ReservationResponse reservation;
        WaitlistEntryResponse waitlistEntry;
        String message;
    }
}
