package com.example.demo.modules.monitor.services;

import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.MonitorFilterCriteria;
import com.example.demo.modules.monitor.dto.MonitorStatisticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IManagerMonitorService {
    Page<ApiResponse> getAllMonitors(MonitorFilterCriteria criteria, Pageable pageable);
    MonitorStatisticsDTO getMonitorStatistics();
}
