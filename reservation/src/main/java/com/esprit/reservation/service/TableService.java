package com.esprit.reservation.service;

import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.entity.TableStatus;
import com.esprit.reservation.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableService {

    private final RestaurantTableRepository tableRepository;

    public List<RestaurantTable> getAllTables() {
        return tableRepository.findByActiveTrue();
    }

    public List<RestaurantTable> getTablesByRoom(Long roomId) {
        return tableRepository.findByRoomIdAndActiveTrue(roomId);
    }

    public Optional<RestaurantTable> getTableById(Long id) {
        return tableRepository.findById(id);
    }

    @Transactional
    public RestaurantTable createTable(RestaurantTable table) {
        table.setActive(true);
        if (table.getStatus() == null) {
            table.setStatus(TableStatus.AVAILABLE);
        }
        return tableRepository.save(table);
    }

    /**
     * Manual status changes are only meant for genuinely physical states (table broken,
     * closed for cleaning) — RESERVED/OCCUPIED are derived automatically from the reservation
     * lifecycle (see ReservationService) and must not be set or cleared by hand here.
     */
    @Transactional
    public RestaurantTable updateTableStatus(Long id, TableStatus status) {
        RestaurantTable table = getTableById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found with ID: " + id));

        TableStatus current = table.getStatus();

        if (status == TableStatus.RESERVED || status == TableStatus.OCCUPIED) {
            throw new IllegalArgumentException(
                    "RESERVED and OCCUPIED are set automatically by the reservation lifecycle and cannot be set manually.");
        }
        if ((current == TableStatus.RESERVED || current == TableStatus.OCCUPIED) && status != current) {
            throw new IllegalArgumentException(
                    "This table is tied to an active reservation — seat, complete, or cancel that reservation instead of changing the table status directly.");
        }

        table.setStatus(status);
        return tableRepository.save(table);
    }
}
