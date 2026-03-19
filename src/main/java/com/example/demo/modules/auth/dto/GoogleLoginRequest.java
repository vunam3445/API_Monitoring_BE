package com.example.demo.modules.auth.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String credential; // Đây là cái ID Token mà Google trả về cho FE
}
