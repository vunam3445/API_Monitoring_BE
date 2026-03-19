package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

public class ForbidenException extends BaseException{
    public ForbidenException(String message){
        super(message, HttpStatus.FORBIDDEN);
    }
}
