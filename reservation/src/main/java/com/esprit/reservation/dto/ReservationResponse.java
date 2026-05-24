package com.esprit.reservation.dto;

import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.domain.ReservationCode;
import com.esprit.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ReservationResponse(
    Long id,
    ReservationCode reservationCode,
    CustomerResponse customer,
    TableResponse table,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    GuestsCount guestsCount,
    ReservationStatus status,
    String source,
    String specialRequests,
    String cancellationReason,
    List<ReservationStatusHistoryResponse> statusHistory,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
