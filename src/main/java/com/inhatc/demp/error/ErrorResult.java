package com.inhatc.demp.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResult {

    private String errorMessage;
    private HttpStatus errorCode;
}
