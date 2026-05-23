package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class MonitorNotFoundException extends BaseException{
    public MonitorNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
