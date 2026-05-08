package com.example.demo.modules.user.dto;

import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String company;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private String planType;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime currentPeriodEnd;
}
