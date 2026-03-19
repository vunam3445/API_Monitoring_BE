package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class GoogleAuthenticationException extends BaseException{
    public GoogleAuthenticationException( String message){
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
