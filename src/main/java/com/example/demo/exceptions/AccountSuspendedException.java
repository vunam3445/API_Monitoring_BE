package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class AccountSuspendedException extends BaseException{

    public AccountSuspendedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
