package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends BaseException{
    public SubscriptionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
