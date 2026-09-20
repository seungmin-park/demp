package com.inhatc.demp.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status) { this.status = status; }
    public HttpStatus getStatus() { return status; }
}
