package com.esprit.employee.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode code;

    public ApplicationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApplicationException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
