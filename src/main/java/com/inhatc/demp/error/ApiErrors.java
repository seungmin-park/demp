package com.inhatc.demp.error;

import org.springframework.http.HttpStatus;

public final class ApiErrors {
    private ApiErrors() {}
    public static ErrorResult body(HttpStatus status, String path) {
        String message;
        switch (status) {
            case BAD_REQUEST: message = "Invalid request"; break;
            case UNAUTHORIZED: message = "Authentication required"; break;
            case FORBIDDEN: message = "Access denied"; break;
            case NOT_FOUND: message = "Resource not found"; break;
            case CONFLICT: message = "Resource already exists"; break;
            case TOO_MANY_REQUESTS: message = "Too many requests"; break;
            default: message = "Internal server error";
        }
        return new ErrorResult(message, status.value(), path);
    }
}
