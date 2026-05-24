package com.esprit.reservation.dto;

import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.entity.WaitlistStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record WaitlistEntryResponse(
    Long id,
    CustomerResponse customer,
    LocalDate requestedDate,
    LocalTime requestedTime,
    GuestsCount guestsCount,
    WaitlistStatus status,
    Integer priority,
    String notes,
    LocalDateTime createdAt
) {}
