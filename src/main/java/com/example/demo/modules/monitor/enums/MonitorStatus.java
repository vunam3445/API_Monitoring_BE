package com.example.demo.modules.monitor.enums;

import lombok.Getter;

@Getter
public enum MonitorStatus {
    HEALTHY("Hệ thống hoạt động tốt"),
    WARNING("Hệ thống phản hồi chậm hoặc có cảnh báo"),
    DOWN("Hệ thống không hoạt động"),
    PAUSED("Hệ thống đang tạm dừng");

    private final String description;

    MonitorStatus(String description) {
        this.description = description;
    }
}
