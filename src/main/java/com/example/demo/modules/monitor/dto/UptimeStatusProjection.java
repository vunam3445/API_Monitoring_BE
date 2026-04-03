package com.example.demo.modules.monitor.dto;

import java.time.LocalDateTime;

public interface UptimeStatusProjection {
    LocalDateTime getTime();
    Integer getLatencyMs();
    Boolean getIsUp();
}
