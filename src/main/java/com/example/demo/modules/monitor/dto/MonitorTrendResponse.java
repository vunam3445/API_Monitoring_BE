package com.example.demo.modules.monitor.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MonitorTrendResponse {
    private UUID monitorId;
    private String range;
    private List<Integer> points;
}
