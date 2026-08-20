package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class FeatureInUseException extends BaseException {
    public FeatureInUseException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
