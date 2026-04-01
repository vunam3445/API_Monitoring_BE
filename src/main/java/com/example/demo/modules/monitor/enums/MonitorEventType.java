package com.example.demo.modules.monitor.enums;

import lombok.Getter;

@Getter
public enum MonitorEventType {
    HEALTH_CHECK_PASSED("Kiểm tra thành công"),
    SLOW_RESPONSE("Phản hồi chậm"),
    API_FAILURE("API lỗi"),
    TIMEOUT("Hết thời gian phản hồi"),
    RECOVERED("Hệ thống đã khôi phục");

    private final String description;

    MonitorEventType(String description) {
        this.description = description;
    }
}
