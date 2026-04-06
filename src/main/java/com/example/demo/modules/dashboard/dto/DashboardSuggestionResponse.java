package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSuggestionResponse {
    private String title;
    private String message;
    private UUID relatedMonitorId;
    private String relatedMonitorName;
    private String suggestionType; // HIGH_ERROR_RATE, DOWNTIME, OPTIMIZATION
}
