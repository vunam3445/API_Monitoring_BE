package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.AlertConfig;
import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.repositories.AlertConfigRepository;
import com.example.demo.modules.alert.repositories.AlertDeliveryRepository;
import com.example.demo.modules.user.entities.UserSetting;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dispatcher gửi thông báo khi có incident.
 *
 * Sử dụng Strategy Pattern:
 * - strategies: Map<AlertChannelType, NotificationStrategy> được inject tự động
 * bởi Spring từ tất cả bean implements NotificationStrategy.
 * - Để thêm kênh mới: chỉ cần tạo class implements NotificationStrategy
 * + @Service.
 * Dispatcher này KHÔNG cần sửa đổi (Open-Closed Principle).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertNotificationDispatcher {

    private final AlertConfigRepository alertConfigRepository;
    private final UserSettingRepository userSettingRepository;
    private final AlertDeliveryRepository alertDeliveryRepository;
    private final AlertDeliveryService deliveryService;

    /**
     * Spring inject toàn bộ bean implements NotificationStrategy vào List,
     * sau đó ta chuyển sang Map để lookup O(1) theo channel type.
     */
    private final List<NotificationStrategy> strategyList;

    private Map<AlertChannelType, NotificationStrategy> strategyMap() {
        return strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::supportedChannel, Function.identity()));
    }

    @Async
    public void dispatch(Incident incident) {
        // 0. Kiểm tra Mute
        if (Boolean.TRUE.equals(incident.getMonitor().getIsMuted())) {
            log.info("Dispatcher: Monitor {} is muted. Skipping notification.", incident.getMonitor().getName());
            return;
        }

        log.info("Dispatcher: processing notification for incident {}", incident.getId());

        UUID monitorId = incident.getMonitor().getId();
        UUID userId = incident.getMonitor().getUserId();

        Map<AlertChannelType, NotificationStrategy> strategies = strategyMap();

        // 1. Gửi theo cài đặt mặc định của User (UserSetting)
        UserSetting setting = userSettingRepository.findByIdWithUserAndPlan(userId).orElse(null);
        if (setting != null) {
            dispatchUserSettings(setting, incident, strategies);
        }

        // 2. Gửi theo cấu hình riêng của Monitor (AlertConfig)
        List<AlertConfig> configs = alertConfigRepository.findAllByMonitorIdAndIsEnabledTrue(monitorId);
        configs.forEach(config -> dispatchToChannel(config.getType(), config.getDestination(), incident, strategies));
    }

    /**
     * Xử lý các kênh được bật trong UserSetting.
     */
    private void dispatchUserSettings(UserSetting setting, Incident incident,
            Map<AlertChannelType, NotificationStrategy> strategies) {
        if (setting.isEmailAlertsEnabled()) {
            String email = setting.getAlertEmail() != null && !setting.getAlertEmail().isBlank()
                    ? setting.getAlertEmail()
                    : setting.getUser().getEmail();
            if (email != null && !email.isBlank()) {
                dispatchToChannel(AlertChannelType.EMAIL, email, incident, strategies);
            }
        }

        if (setting.isSlackEnabled() && setting.getSlackWebhookUrl() != null
                && !setting.getSlackWebhookUrl().isBlank()) {
            dispatchToChannel(AlertChannelType.SLACK, setting.getSlackWebhookUrl(), incident, strategies);
        }
    }

    /**
     * Gửi thông báo đến một kênh cụ thể thông qua Strategy tương ứng.
     * Ghi lại kết quả vào AlertDelivery để audit.
     */
    private void dispatchToChannel(AlertChannelType channel, String destination, Incident incident,
            Map<AlertChannelType, NotificationStrategy> strategies) {
        NotificationStrategy strategy = strategies.get(channel);
        if (strategy == null) {
            log.warn("No strategy registered for channel: {}. Skipping.", channel);
            return;
        }

        log.debug("Dispatching {} to {}", channel, destination);
        AlertDelivery delivery = deliveryService.createPending(incident, channel, destination);

        long startTime = System.currentTimeMillis();
        try {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                strategy.sendRecovery(destination, incident);
            } else {
                strategy.sendIncident(destination, incident);
            }
            int latency = (int) (System.currentTimeMillis() - startTime);
            deliveryService.markSuccessWithLatency(delivery, channel + " notification sent successfully", latency);
        } catch (Exception e) {
            log.error("Failed to dispatch {} to {}: {}", channel, destination, e.getMessage());
            deliveryService.markFailed(delivery, e.getMessage());
        }
    }

    @Async
    @Transactional
    public void retryDelivery(UUID deliveryId) {
        // 1. Fetch lại delivery kèm theo Incident và Monitor trong session mới
        AlertDelivery delivery = alertDeliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Retry failed: Delivery {} not found", deliveryId);
            return;
        }

        log.info("Retrying delivery {} for incident {}", delivery.getId(), delivery.getIncident().getId());
        Map<AlertChannelType, NotificationStrategy> strategies = strategyMap();
        NotificationStrategy strategy = strategies.get(delivery.getChannel());

        if (strategy == null) {
            log.warn("No strategy registered for channel: {}. Skipping.", delivery.getChannel());
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            // Đảm bảo incident được load (Hibernate sẽ tự động load do @Transactional và
            // join fetch hoặc access)
            Incident incident = delivery.getIncident();

            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                strategy.sendRecovery(delivery.getDestination(), incident);
            } else {
                strategy.sendIncident(delivery.getDestination(), incident);
            }
            int latency = (int) (System.currentTimeMillis() - startTime);
            deliveryService.markSuccessWithLatency(delivery,
                    delivery.getChannel() + " notification retried successfully", latency);
        } catch (Exception e) {
            log.error("Failed to retry {} to {}: {}", delivery.getChannel(), delivery.getDestination(), e.getMessage());
            deliveryService.markFailed(delivery, e.getMessage());
        }
    }
}
