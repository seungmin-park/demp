package com.inhatc.demp.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException() { super(HttpStatus.NOT_FOUND); }
}
