package com.esprit.reservation.service;

import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.ReservationStatus;
import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.repository.ReservationRepository;
import com.esprit.reservation.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationStatisticsService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final AvailabilityService availabilityService;

    public Map<String, Object> getDailyStats(LocalDate date) {
        Map<String, Object> stats = buildStats(reservationRepository.findByReservationDate(date), date);
        stats.put("date", date);
        return stats;
    }

    public Map<String, Object> getStatsForRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = buildStats(
                reservationRepository.findByReservationDateBetween(startDate, endDate), LocalDate.now());
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        return stats;
    }

    private Map<String, Object> buildStats(List<Reservation> reservations, LocalDate availabilityReferenceDate) {
        List<RestaurantTable> allTables = tableRepository.findByActiveTrue();

        long total = reservations.size();
        long confirmed = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
        long seated = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.SEATED).count();
        long completed = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED).count();
        long cancelled = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        long noShows = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.NO_SHOW).count();

        // Calculate available tables right now (a point-in-time snapshot, independent of the reporting period)
        LocalTime now = LocalTime.now();
        List<RestaurantTable> availableTablesNow = availabilityService
                .findAvailableTables(availabilityReferenceDate, now, now.plusHours(2), GuestsCount.of(1));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReservations", total);
        stats.put("confirmedCount", confirmed);
        stats.put("seatedCount", seated);
        stats.put("completedCount", completed);
        stats.put("cancelledCount", cancelled);
        stats.put("noShowCount", noShows);
        stats.put("totalTablesCount", allTables.size());
        stats.put("availableTablesRightNowCount", availableTablesNow.size());

        return stats;
    }
}
