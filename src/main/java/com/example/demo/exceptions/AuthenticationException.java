package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException{
    public AuthenticationException(String message){
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
