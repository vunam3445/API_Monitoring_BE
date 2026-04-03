package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;

import java.util.Optional;
import java.util.UUID;

public interface IIncidentService {
    /**
     * Phân tích kết quả check và tạo/cập nhật incident nếu cần.
     * @return Incident data if created or updated, otherwise empty
     */
    Optional<Incident> processCheckResult(Monitor monitor, UptimeLogs log);

    /**
     * Resolve tất cả incident đang active của monitor khi nó recover.
     */
    void resolveActiveIncidents(Monitor monitor, UptimeLogs log);

    void acknowledge(UUID userId, UUID id);
    
    void resolve(UUID userId, UUID id);
    
    void delete(UUID userId, UUID id);

    Optional<Incident> findById(UUID id);
}
