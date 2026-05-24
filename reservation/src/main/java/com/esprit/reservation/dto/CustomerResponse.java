package com.esprit.reservation.dto;

import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.PhoneNumber;

public record CustomerResponse(
    Long id,
    String keycloakUserId,
    String fullName,
    EmailAddress email,
    PhoneNumber phone
) {}
