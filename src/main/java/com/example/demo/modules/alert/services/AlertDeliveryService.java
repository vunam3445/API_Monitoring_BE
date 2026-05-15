package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertDeliveryLogDTO;
import com.example.demo.modules.alert.dto.AlertDeliveryStatsDTO;
import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import com.example.demo.modules.alert.repositories.AlertDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertDeliveryService implements IAlertDeliveryService {
    private final AlertDeliveryRepository deliveryRepository;
    private final AlertNotificationDispatcher notificationDispatcher;

    public AlertDeliveryService(AlertDeliveryRepository deliveryRepository,
            @Lazy AlertNotificationDispatcher notificationDispatcher) {
        this.deliveryRepository = deliveryRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public AlertDelivery createPending(Incident incident, AlertChannelType channel, String destination) {
        AlertDelivery delivery = AlertDelivery.builder()
                .incident(incident)
                .channel(channel)
                .destination(destination)
                .status(AlertDeliveryStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void markSuccess(AlertDelivery delivery, String message) {
        markSuccessWithLatency(delivery, message, null);
    }

    @Transactional
    @Override
    public void markSuccessWithLatency(AlertDelivery delivery, String message, Integer latencyMs) {
        delivery.setStatus(AlertDeliveryStatus.SENT);
        delivery.setMessage(message);
        delivery.setErrorMessage(null); // Xóa lỗi cũ nếu có
        delivery.setSentAt(LocalDateTime.now());
        delivery.setLatencyMs(latencyMs);
        delivery.setCreatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void markFailed(AlertDelivery delivery, String errorMessage) {
        delivery.setStatus(AlertDeliveryStatus.FAILED);
        delivery.setErrorMessage(errorMessage);
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        delivery.setCreatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    @Override
    public AlertDeliveryStatsDTO getStats() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);

        long totalSent24h = deliveryRepository.countByStatusAndCreatedAtAfter(AlertDeliveryStatus.SENT, since24h);
        long totalFailed24h = deliveryRepository.countByStatusAndCreatedAtAfter(AlertDeliveryStatus.FAILED, since24h);
        long total24h = totalSent24h + totalFailed24h;

        double successRate = total24h == 0 ? 100.0 : (double) totalSent24h * 100 / total24h;
        String mostCommonError = deliveryRepository.findMostCommonError(since24h);
        long queueDepth = deliveryRepository.countByStatus(AlertDeliveryStatus.PENDING);
        Double avgLatency = deliveryRepository.getAverageLatency(since24h);

        return AlertDeliveryStatsDTO.builder()
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .totalFailures24h(totalFailed24h)
                .totalSent24h(totalSent24h)
                .mostCommonError(mostCommonError != null ? mostCommonError : "None")
                .queueDepth(queueDepth)
                .avgLatencyMs(avgLatency != null ? Math.round(avgLatency * 10.0) / 10.0 : 0.0)
                .build();
    }

    @Override
    public Page<AlertDeliveryLogDTO> getLogs(Pageable pageable, AlertChannelType channel, AlertDeliveryStatus status,
            String search) {
        return deliveryRepository.findWithFilters(channel, status, search, pageable)
                .map(this::convertToLogDTO);
    }

    @Override
    public void retryAllFailed24h() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        List<AlertDelivery> failed = deliveryRepository.findAllByStatusAndCreatedAtAfter(AlertDeliveryStatus.FAILED,
                since24h);
        failed.forEach(d -> notificationDispatcher.retryDelivery(d.getId()));
    }

    @Override
    public void retry(UUID deliveryId) {
        notificationDispatcher.retryDelivery(deliveryId);
    }

    private AlertDeliveryLogDTO convertToLogDTO(AlertDelivery delivery) {
        String userName = "System";
        if (delivery.getIncident().getMonitor().getUser() != null) {
            userName = delivery.getIncident().getMonitor().getUser().getFullName();
            if (userName == null || userName.isBlank()) {
                userName = delivery.getIncident().getMonitor().getUser().getEmail();
            }
        }

        return AlertDeliveryLogDTO.builder()
                .id(delivery.getId())
                .timestamp(delivery.getCreatedAt())
                .monitorName(delivery.getIncident().getMonitor().getName())
                .userName(userName)
                .channel(delivery.getChannel())
                .status(delivery.getStatus())
                .destination(delivery.getDestination())
                .errorMessage(delivery.getErrorMessage())
                .latencyMs(delivery.getLatencyMs())
                .retryCount(delivery.getRetryCount())
                .build();
    }
}
