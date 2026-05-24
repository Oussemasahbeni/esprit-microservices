package com.esprit.reservation.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public record PhoneNumber(@JsonValue String value) implements Serializable {
    @JsonCreator
    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
}
