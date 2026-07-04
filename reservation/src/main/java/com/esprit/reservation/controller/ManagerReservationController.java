package com.esprit.reservation.controller;

import com.esprit.reservation.dto.ReservationResponse;
import com.esprit.reservation.dto.WaitlistEntryResponse;
import com.esprit.reservation.mapper.ReservationMapper;
import com.esprit.reservation.mapper.WaitlistMapper;
import com.esprit.reservation.service.ReservationService;
import com.esprit.reservation.service.ReservationStatisticsService;
import com.esprit.reservation.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerReservationController {

    private final ReservationService reservationService;
    private final WaitlistService waitlistService;
    private final ReservationStatisticsService statisticsService;
    private final ReservationMapper reservationMapper;
    private final WaitlistMapper waitlistMapper;

    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
            return ResponseEntity.ok(reservationMapper.toResponseList(
                    reservationService.getReservationsByDateRange(startDate, endDate)));
        }
        if (date != null) {
            return ResponseEntity.ok(reservationMapper.toResponseList(reservationService.getReservationsByDate(date)));
        }
        return ResponseEntity.ok(reservationMapper.toResponseList(reservationService.getAllReservations()));
    }

    @PatchMapping("/reservations/{id}/seat")
    @Transactional
    public ResponseEntity<ReservationResponse> seatReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationMapper.toResponse(reservationService.seatReservation(id, "MANAGER")));
    }

    @PatchMapping("/reservations/{id}/complete")
    @Transactional
    public ResponseEntity<ReservationResponse> completeReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationMapper.toResponse(reservationService.completeReservation(id, "MANAGER")));
    }

    @PatchMapping("/reservations/{id}/no-show")
    @Transactional
    public ResponseEntity<ReservationResponse> noShowReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationMapper.toResponse(reservationService.markNoShow(id, "MANAGER")));
    }

    @PatchMapping("/reservations/{id}/assign-table/{tableId}")
    @Transactional
    public ResponseEntity<ReservationResponse> assignTable(@PathVariable Long id, @PathVariable Long tableId) {
        return ResponseEntity.ok(reservationMapper.toResponse(reservationService.assignTable(id, tableId, "MANAGER")));
    }

    @GetMapping("/waitlist")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WaitlistEntryResponse>> getWaitlist() {
        return ResponseEntity.ok(waitlistMapper.toResponseList(waitlistService.getActiveWaitlist()));
    }

    @PostMapping("/waitlist/{id}/promote")
    public ResponseEntity<Void> promoteWaitlistEntry(@PathVariable Long id) {
        waitlistService.promoteWaitlistEntry(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/waitlist/{id}/cancel")
    @Transactional
    public ResponseEntity<WaitlistEntryResponse> cancelWaitlistEntry(@PathVariable Long id) {
        return ResponseEntity.ok(waitlistMapper.toResponse(waitlistService.cancelWaitlistEntry(id)));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
            return ResponseEntity.ok(statisticsService.getStatsForRange(startDate, endDate));
        }
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(statisticsService.getDailyStats(targetDate));
    }
}
