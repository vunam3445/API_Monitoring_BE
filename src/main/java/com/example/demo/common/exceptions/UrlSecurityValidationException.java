package com.example.demo.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception được ném ra khi URL vi phạm chính sách bảo mật của hệ thống.
 * Chống SSRF, DNS Rebinding, CRLF Injection.
 * HTTP Status: 400 BAD REQUEST
 */
public class UrlSecurityValidationException extends BaseException {
    public UrlSecurityValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
