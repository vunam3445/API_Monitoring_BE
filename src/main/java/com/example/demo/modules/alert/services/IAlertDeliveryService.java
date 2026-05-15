package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertDeliveryLogDTO;
import com.example.demo.modules.alert.dto.AlertDeliveryStatsDTO;
import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IAlertDeliveryService {
    AlertDelivery createPending(Incident incident, AlertChannelType channel, String destination);
    void markSuccess(AlertDelivery delivery, String message);
    void markSuccessWithLatency(AlertDelivery delivery, String message, Integer latencyMs);
    void markFailed(AlertDelivery delivery, String errorMessage);

    AlertDeliveryStatsDTO getStats();
    Page<AlertDeliveryLogDTO> getLogs(Pageable pageable, AlertChannelType channel, AlertDeliveryStatus status, String search);
    void retryAllFailed24h();
    void retry(UUID deliveryId);
}
