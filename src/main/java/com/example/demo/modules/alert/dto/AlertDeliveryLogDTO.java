package com.example.demo.modules.alert.dto;

import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
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
public class AlertDeliveryLogDTO {
    private UUID id;
    private LocalDateTime timestamp;
    private String monitorName;
    private String userName;
    private AlertChannelType channel;
    private AlertDeliveryStatus status;
    private String destination;
    private String errorMessage;
    private Integer latencyMs;
    private Integer retryCount;
}
