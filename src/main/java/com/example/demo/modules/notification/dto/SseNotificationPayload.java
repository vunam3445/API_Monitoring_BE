package com.example.demo.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseNotificationPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID userId;
    private UserNotificationResponse notification;
}
