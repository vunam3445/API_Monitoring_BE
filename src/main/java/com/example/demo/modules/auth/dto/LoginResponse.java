package com.example.demo.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder // Giúp khởi tạo object theo phong cách Fluent API
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    // Nên trả về thêm một ít thông tin user để FE hiển thị (tên, email)
    private UUID userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String planType;
    private String role;
}
