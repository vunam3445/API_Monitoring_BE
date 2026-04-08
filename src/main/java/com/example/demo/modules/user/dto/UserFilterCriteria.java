package com.example.demo.modules.user.dto;

import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.enums.UserStatus;
import lombok.Data;

@Data
public class UserFilterCriteria {
    private String email;
    private String fullName;
    private UserStatus status;
    private UserRole role;
    private String planType;
}
