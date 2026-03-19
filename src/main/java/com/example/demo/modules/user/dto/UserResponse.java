package com.example.demo.modules.user.dto;

import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String company;
    private String avatarUrl;

    // Bạn có thể thêm các thông tin từ UserSetting nếu cần
    // private UserSettingResponse settings;
}