package com.bizengine.authorization.service;

import org.springframework.http.HttpStatus;

public class AuthorizationDeniedException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public AuthorizationDeniedException(String code, String message) {
        super(message);
        this.status = HttpStatus.FORBIDDEN;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
