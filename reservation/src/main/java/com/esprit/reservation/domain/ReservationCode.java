package com.esprit.reservation.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public record ReservationCode(@JsonValue String value) implements Serializable {
    @JsonCreator
    public ReservationCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Reservation code cannot be null or blank");
        }
        if (!value.matches("^RES-\\d{8}-[A-Z0-9]{8}$")) {
            throw new IllegalArgumentException("Invalid reservation code format: " + value);
        }
    }

    public static ReservationCode of(String value) {
        return new ReservationCode(value);
    }
}
