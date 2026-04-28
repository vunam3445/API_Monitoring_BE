package com.example.demo.modules.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserMonitorAlertResponse {
    private long totalAlert;
    private long totalIncident;


}
