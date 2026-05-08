package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotSubscriptionActive extends BaseException{
    public UserNotSubscriptionActive(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
