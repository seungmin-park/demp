package com.inhatc.demp.controller;

import com.inhatc.demp.error.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.UnexpectedTypeException;

@Slf4j
@RestControllerAdvice
public class ExController {

    @ExceptionHandler
    public ErrorResult illegalExHandle(HttpServletRequest request, IllegalStateException e) {
//        log.error("[exceptionHandle] ex", e);
        return new ErrorResult(e.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI());
    }

    @ExceptionHandler
    public ErrorResult UnexpectedTypeHandle(HttpServletRequest request, UnexpectedTypeException e) {
        log.error("[exceptionHandle] ex", e);
        return new ErrorResult(e.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI());
    }

    @ExceptionHandler
    public ErrorResult bindingExHandle(HttpServletRequest request, BindException e) {
        log.error("[exceptionHandle] ex", e);
        return new ErrorResult(e.getFieldError().getDefaultMessage(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI());
    }
}
