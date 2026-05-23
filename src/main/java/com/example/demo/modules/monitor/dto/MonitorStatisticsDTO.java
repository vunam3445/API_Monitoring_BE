package com.example.demo.modules.monitor.dto;

import lombok.Data;

@Data
public class MonitorStatisticsDTO {
    private long totalMonitors;    // Tổng số monitor
    private long activeMonitors;   // Số monitor đang hoạt động (Up)
    private long downMonitors;     // Số monitor đang bị lỗi (Down)

    // Platform Capacity (phần trăm sử dụng tài nguyên)
    private double platformCapacity;

    // Bạn có thể thêm tỷ lệ phần trăm nếu cần
    // private double availabilityRate;
}