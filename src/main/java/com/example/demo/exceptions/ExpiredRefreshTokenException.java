package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class ExpiredRefreshTokenException extends BaseException{// token het han
    public ExpiredRefreshTokenException(String message){
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
