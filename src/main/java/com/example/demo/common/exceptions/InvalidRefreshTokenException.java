package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BaseException{ // refresh token khong hop le
    public InvalidRefreshTokenException(String message){
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
