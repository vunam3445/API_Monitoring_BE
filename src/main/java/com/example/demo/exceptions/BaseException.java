package com.example.demo.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;
@Data
public abstract class BaseException extends RuntimeException {
    private HttpStatus httpStatus;
    public BaseException(String message, HttpStatus httpStatus){
        super(message);
        this.httpStatus = httpStatus;
    }

}
