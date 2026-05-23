package com.example.demo.modules.monitor.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class MonitorFilterCriteria {
    private String search;
    private String lastStatus;
    private Boolean isActive;
    private UUID userId;
}
