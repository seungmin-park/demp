package com.inhatc.demp.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResult {

    private String errorMessage;
    private int errorCode;
    private String instance;
}
