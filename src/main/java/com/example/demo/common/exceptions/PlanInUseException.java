package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class PlanInUseException extends BaseException {
    public PlanInUseException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
