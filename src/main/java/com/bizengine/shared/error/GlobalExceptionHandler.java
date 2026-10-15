package com.bizengine.shared.error;

import com.bizengine.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors()
                .stream().map(this::toViolation).toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more request fields are invalid.", request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more values violate validation constraints.", request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "The request body is missing or malformed.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request failure requestId={} path={}",
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE), request.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", request, List.of());
    }

    private ApiErrorResponse.FieldViolation toViolation(FieldError error) {
        return new ApiErrorResponse.FieldViolation(error.getField(),
                error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                      HttpServletRequest request, List<ApiErrorResponse.FieldViolation> violations) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), code, message,
                request.getRequestURI(), id == null ? null : id.toString(), violations);
        return ResponseEntity.status(status).body(body);
    }
}
