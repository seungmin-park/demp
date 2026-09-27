package com.inhatc.demp.controller;

import com.inhatc.demp.error.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class ExController {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResult> business(ApiException ex, HttpServletRequest request) {
        return error(ex.getStatus(), request);
    }
    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResult> invalid(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, request);
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResult> unauthenticated(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, request);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResult> forbidden(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, request);
    }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResult> notFound(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, request);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> internal(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    private ResponseEntity<ErrorResult> error(HttpStatus status, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrors.body(status, request.getRequestURI()));
    }
}
