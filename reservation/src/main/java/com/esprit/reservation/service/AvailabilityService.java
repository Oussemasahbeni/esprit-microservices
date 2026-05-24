package com.esprit.reservation.service;

import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.repository.ReservationRepository;
import com.esprit.reservation.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public List<RestaurantTable> findAvailableTables(LocalDate date, LocalTime startTime, LocalTime endTime, GuestsCount guestsCount) {
        // Find tables that have enough capacity and are active
        List<RestaurantTable> candidateTables = tableRepository
                .findByCapacityGreaterThanEqualAndActiveTrueOrderByCapacityAsc(guestsCount);

        // Find active reservations overlapping with the requested interval
        List<Reservation> overlappingReservations = reservationRepository
                .findOverlappingReservations(date, startTime, endTime);

        // Extract occupied table IDs
        Set<Long> occupiedTableIds = overlappingReservations.stream()
                .map(r -> r.getTable() != null ? r.getTable().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Return candidate tables that are not occupied
        return candidateTables.stream()
                .filter(table -> !occupiedTableIds.contains(table.getId()))
                .collect(Collectors.toList());
    }
}
