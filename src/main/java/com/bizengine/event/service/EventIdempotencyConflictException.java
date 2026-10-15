package com.bizengine.event.service;

import com.bizengine.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class EventIdempotencyConflictException extends ApiException {
    public EventIdempotencyConflictException() {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "This idempotency key was already used with a different event body.");
    }
}
