package com.example.demo.modules.dashboard.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystemHealthResponse {
    /** CPU usage as percentage (0-100) */
    double cpuUsage;
    /** RAM usage as percentage (0-100) */
    double ramUsage;
    /** Disk usage as percentage (0-100) */
    double diskUsage;
    /** Number of pending messages in the monitoring queue */
    int pendingQueue;
    /** Status of execution workers */
    boolean isWorkersRunning;
}
