package com.esprit.delivery.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record ApiErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldErrorItem> fieldErrors,
        List<GlobalErrorItem> globalErrors
) {

    public record FieldErrorItem(
            String code,
            String property,
            String message,
            Object rejectedValue
    ) {
    }

    public record GlobalErrorItem(
            String code,
            String message
    ) {
    }
}
