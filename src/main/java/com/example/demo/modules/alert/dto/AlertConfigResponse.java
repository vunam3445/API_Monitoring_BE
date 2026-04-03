package com.example.demo.modules.alert.dto;

import com.example.demo.modules.alert.enums.AlertChannelType;
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
public class AlertConfigResponse {
    private UUID id;
    private UUID monitorId;
    private AlertChannelType type;
    private String destination;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
