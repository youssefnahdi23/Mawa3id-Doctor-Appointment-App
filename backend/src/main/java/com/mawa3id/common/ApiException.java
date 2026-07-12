package com.mawa3id.common;

import org.springframework.http.HttpStatus;

/**
 * Base exception carrying an HTTP status so the {@link GlobalExceptionHandler}
 * can translate it into a consistent {@link ApiError} response.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
