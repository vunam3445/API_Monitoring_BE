package com.example.demo.modules.notification.dto;

import com.example.demo.modules.notification.enums.NotificationLevel;
import com.example.demo.modules.notification.enums.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO nhận yêu cầu gửi thông báo từ Admin.
 */
@Data
public class SendNotificationRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @NotNull(message = "Loại đối tượng nhận không được để trống")
    private TargetType targetType;

    /** Email (nếu SINGLE) hoặc tên gói (nếu PLAN), null nếu ALL */
    private String targetValue;

    @NotNull(message = "Mức độ thông báo không được để trống")
    private NotificationLevel level;

    private boolean sendWeb = true;

    private boolean sendEmail = true;
}
