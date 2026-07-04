package com.esprit.reservation.controller;

import com.esprit.reservation.dto.MenuSnapshotResponse;
import com.esprit.reservation.dto.ReservationResponse;
import com.esprit.reservation.dto.TableResponse;
import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.domain.ReservationCode;
import com.esprit.reservation.dto.CreateReservationRequest;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.TableMapper;
import com.esprit.reservation.service.AvailabilityService;
import com.esprit.reservation.service.MenuSnapshotService;
import com.esprit.reservation.service.ReservationService;
import com.esprit.reservation.service.ReservationService.BookingResult;
import com.esprit.reservation.entity.Reservation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;
    private final MenuSnapshotService menuSnapshotService;
    private final ReservationMapper reservationMapper;
    private final TableMapper tableMapper;

    @PostMapping
    public ResponseEntity<BookingResult> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        String keycloakUserId = userId != null ? userId : request.getKeycloakUserId();
        BookingResult result = reservationService.createReservation(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                keycloakUserId,
                request.getReservationDate(),
                request.getStartTime(),
                request.getGuestsCount(),
                request.getSpecialRequests(),
                request.getPreOrderItems()
        );
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(reservationMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReservationResponse> getReservationByCode(@PathVariable ReservationCode code) {
        return reservationService.getReservationByCode(code)
                .map(reservationMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Client Cancelled") String reason,
            @RequestAttribute(value = "userId", required = false) String userKeycloakId,
            @RequestAttribute(value = "userRoles", required = false) List<String> userRoles) {
        if (userKeycloakId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Reservation reservation = reservationService.getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));

        boolean isOwner = userKeycloakId.equals(reservation.getKeycloakUserId());
        List<String> roles = userRoles != null ? userRoles : List.of();
        boolean isManagerOrAdmin = roles.stream()
                .anyMatch(role -> role.equalsIgnoreCase("manager") || role.equalsIgnoreCase("admin"));

        if (!isOwner && !isManagerOrAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String cancelledBy = isOwner ? "CLIENT" : "MANAGER";
        return ResponseEntity.ok(reservationMapper.toResponse(
                reservationService.cancelReservation(id, reason, cancelledBy)
        ));
    }

    @GetMapping("/menu")
    public ResponseEntity<MenuSnapshotResponse> getMenuSnapshot() {
        return ResponseEntity.ok(menuSnapshotService.getSnapshot());
    }

    @GetMapping("/availability")
    public ResponseEntity<List<TableResponse>> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam GuestsCount guests) {
        // Assume default reservation slot of 2 hours
        return ResponseEntity.ok(tableMapper.toResponseList(
                availabilityService.findAvailableTables(date, time, time.plusHours(2), guests)
        ));
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestAttribute(value = "userId", required = false) String keycloakId) {
        if (keycloakId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reservationMapper.toResponseList(
                reservationService.getCustomerReservationsByKeycloakId(keycloakId)
        ));
    }
}
