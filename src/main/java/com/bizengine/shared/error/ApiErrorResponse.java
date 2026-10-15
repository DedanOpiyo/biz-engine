package com.bizengine.shared.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(Instant timestamp, int status, String code, String message,
                               String path, String requestId, List<FieldViolation> violations) {
    public record FieldViolation(String field, String message) {}
}
