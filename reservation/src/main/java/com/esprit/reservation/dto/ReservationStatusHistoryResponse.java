package com.esprit.reservation.dto;

import com.esprit.reservation.entity.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationStatusHistoryResponse(
    Long id,
    ReservationStatus oldStatus,
    ReservationStatus newStatus,
    String changedBy,
    String reason,
    LocalDateTime changedAt
) {}
