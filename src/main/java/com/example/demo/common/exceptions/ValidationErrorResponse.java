package com.example.demo.common.exceptions;

// Simple DTO to wrap a single validation error message
public class ValidationErrorResponse {
    private String message;

    public ValidationErrorResponse() {}

    public ValidationErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
