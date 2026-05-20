package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivityResponse {
    private String apiName;
    private String owner;
    private String endpoint;
    private String responseTime;
    private String status; // HEALTHY, TIMEOUT, ERROR
    private String lastCheck;
}
