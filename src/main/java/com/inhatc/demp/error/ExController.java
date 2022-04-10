package com.inhatc.demp.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExController {

    @ExceptionHandler
    public ErrorResult illegalExHandle(IllegalStateException e) {
//        log.error("[exceptionHandle] ex", e);
        return new ErrorResult(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ErrorResult bindingExHandle(BindException e) {
        log.error("[exceptionHandle] ex", e);
        return new ErrorResult("잘못된 형식의 데이터 입니다.", HttpStatus.BAD_REQUEST);
    }
}
