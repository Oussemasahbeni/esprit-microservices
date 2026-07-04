package com.esprit.delivery.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static com.esprit.delivery.exception.ErrorCode.*;
import static java.time.Instant.now;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static String deriveValidationCode(FieldError fieldError) {
        String code = fieldError.getCode();
        if (code == null) return "INVALID_VALUE";
        return switch (code) {
            case "NotBlank", "NotEmpty" -> "REQUIRED_NOT_BLANK";
            case "NotNull" -> "REQUIRED_NOT_NULL";
            case "Size" -> "INVALID_SIZE";
            case "Min" -> "VALUE_TOO_SMALL";
            case "Max" -> "VALUE_TOO_LARGE";
            case "Pattern" -> "REGEX_PATTERN_VALIDATION_FAILED";
            case "Email" -> "INVALID_EMAIL";
            default -> code;
        };
    }

    private static String deriveValidationCode(Annotation annotation) {
        if (annotation == null) return "INVALID_VALUE";
        String name = annotation.annotationType().getSimpleName();
        return switch (name) {
            case "NotBlank", "NotEmpty" -> "REQUIRED_NOT_BLANK";
            case "NotNull" -> "REQUIRED_NOT_NULL";
            case "Size" -> "INVALID_SIZE";
            case "Min" -> "VALUE_TOO_SMALL";
            case "Max" -> "VALUE_TOO_LARGE";
            case "Pattern" -> "REGEX_PATTERN_VALIDATION_FAILED";
            case "Email" -> "INVALID_EMAIL";
            default -> name;
        };
    }

    private static String deriveCodeFromClass(Exception ex) {
        String name = ex.getClass().getSimpleName();
        if (name.endsWith("Exception")) {
            name = name.substring(0, name.length() - "Exception".length());
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException ex, HttpServletRequest request) {
        log.warn("Application exception: {}: {}", ex.getCode(), ex.getMessage(), ex);
        ErrorCode code = ex.getCode();
        return buildResponse(code.getHttpStatus(), code.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        List<ApiErrorResponse.FieldErrorItem> fieldErrors = new ArrayList<>();
        List<ApiErrorResponse.GlobalErrorItem> globalErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.add(new ApiErrorResponse.FieldErrorItem(
                        deriveValidationCode(fieldError),
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()));
            } else {
                globalErrors.add(new ApiErrorResponse.GlobalErrorItem(
                        error.getCode(), error.getDefaultMessage()));
            }
        });
        return buildResponse(BAD_REQUEST, VALIDATION_FAILED.name(),
                "Validation failed for object '%s'. Error count: %d"
                        .formatted(ex.getBindingResult().getObjectName(), ex.getErrorCount()),
                request, fieldErrors, globalErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        List<ApiErrorResponse.FieldErrorItem> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldErrorItem(
                        deriveValidationCode(violation.getConstraintDescriptor().getAnnotation()),
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
                .toList();
        return buildResponse(BAD_REQUEST, VALIDATION_FAILED.name(),
                "Validation failed. Error count: %d".formatted(fieldErrors.size()),
                request, fieldErrors, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Message not readable: {}", ex.getMessage());
        return buildResponse(BAD_REQUEST, MESSAGE_NOT_READABLE.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Argument type mismatch: {}", ex.getMessage());
        String message = "Failed to convert value '%s' to required type '%s'"
                .formatted(ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildResponse(BAD_REQUEST, ARGUMENT_TYPE_MISMATCH.name(), message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {}", ex.getMessage());
        return buildResponse(BAD_REQUEST, MISSING_PARAMETER.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return buildResponse(METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Media type not supported: {}", ex.getMessage());
        return buildResponse(UNSUPPORTED_MEDIA_TYPE, MEDIA_TYPE_NOT_SUPPORTED.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.error("Optimistic locking failure: {}", ex.getMessage(), ex);
        return buildResponse(CONFLICT, OPTIMISTIC_LOCKING_ERROR.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(FORBIDDEN, ACCESS_DENIED.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        String code = deriveCodeFromClass(ex);
        return buildResponse(INTERNAL_SERVER_ERROR, code, ex.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return buildResponse(status, code, message, request, null, null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String code, String message, HttpServletRequest request,
            List<ApiErrorResponse.FieldErrorItem> fieldErrors,
            List<ApiErrorResponse.GlobalErrorItem> globalErrors) {
        var body = new ApiErrorResponse(code, message, request.getRequestURI(), now(), fieldErrors, globalErrors);
        return ResponseEntity.status(status).body(body);
    }
}
