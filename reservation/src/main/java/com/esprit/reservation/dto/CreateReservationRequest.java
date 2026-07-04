package com.esprit.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.PhoneNumber;
import com.esprit.reservation.domain.GuestsCount;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Email is required")
    private EmailAddress email;

    private PhoneNumber phone;

    private String keycloakUserId;

    @NotNull(message = "Reservation date is required")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "Guests count is required")
    private GuestsCount guestsCount;

    private String specialRequests;

    @Valid
    private List<PreOrderItemRequest> preOrderItems;
}
