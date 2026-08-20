package com.example.demo.modules.notification.enums;

public enum TargetType {
    ALL,    // Gửi tới tất cả người dùng
    PLAN,   // Gửi tới nhóm người dùng theo gói cước (FREE, PRO, ENTERPRISE)
    SINGLE  // Gửi tới một người dùng cụ thể theo email
}
