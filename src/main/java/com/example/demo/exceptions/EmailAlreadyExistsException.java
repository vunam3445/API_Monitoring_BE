package com.example.demo.exceptions;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseException{
    public EmailAlreadyExistsException(String message){
        super(message, HttpStatus.BAD_REQUEST);
    }
}
