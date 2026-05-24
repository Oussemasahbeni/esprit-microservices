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
        List<Reservation> reservations = reservationRepository.findByReservationDate(date);
        List<RestaurantTable> allTables = tableRepository.findByActiveTrue();

        long total = reservations.size();
        long confirmed = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
        long seated = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.SEATED).count();
        long completed = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED).count();
        long cancelled = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        long noShows = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.NO_SHOW).count();

        // Calculate available tables right now
        LocalTime now = LocalTime.now();
        List<RestaurantTable> availableTablesNow = availabilityService
                .findAvailableTables(date, now, now.plusHours(2), GuestsCount.of(1));

        Map<String, Object> stats = new HashMap<>();
        stats.put("date", date);
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
