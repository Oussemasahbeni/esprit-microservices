package com.esprit.reservation.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public record GuestsCount(@JsonValue Integer value) implements Serializable {
    @JsonCreator
    public GuestsCount {
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Guests count must be at least 1");
        }
    }

    public static GuestsCount of(Integer value) {
        return new GuestsCount(value);
    }
}
