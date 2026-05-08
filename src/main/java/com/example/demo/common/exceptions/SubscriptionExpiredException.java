package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class SubscriptionExpiredException extends BaseException{
    public SubscriptionExpiredException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
