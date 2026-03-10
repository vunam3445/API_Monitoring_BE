package com.example.demo.dto.request;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String credential; // Đây là cái ID Token mà Google trả về cho FE
}