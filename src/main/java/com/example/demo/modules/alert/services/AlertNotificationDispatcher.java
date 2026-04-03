package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.AlertConfig;
import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.repositories.AlertConfigRepository;
import com.example.demo.modules.user.entities.UserSetting;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertNotificationDispatcher {

    private final AlertConfigRepository alertConfigRepository;
    private final UserSettingRepository userSettingRepository;
    private final EmailSenderService emailSender;
    private final SlackWebhookSenderService slackSender;
    private final AlertDeliveryService deliveryService;

    @Async
    public void dispatch(Incident incident) {
        log.info("Dispatcher: processing notification for incident {}", incident.getId());

        UUID monitorId = incident.getMonitor().getId();
        UUID userId = incident.getMonitor().getUserId();

        // 1. Get User Notification Preference (source: UserSetting as requested)
        UserSetting setting = userSettingRepository.findByIdWithUserAndPlan(userId).orElse(null);

        // 2. Get Monitor-level Alert Configs
        List<AlertConfig> configs = alertConfigRepository.findAllByMonitorIdAndIsEnabledTrue(monitorId);

        // 3. Dispatch to Email
        if (setting != null && setting.isEmailAlertsEnabled()) {
            String targetEmail = setting.getAlertEmail() != null && !setting.getAlertEmail().isBlank()
                    ? setting.getAlertEmail()
                    : setting.getUser().getEmail();

            if (targetEmail != null && !targetEmail.isBlank()) {
                sendToEmail(targetEmail, incident);
            }
        }

        // Dispatch to all monitor-specific EMAIL configs
        configs.stream()
                .filter(c -> c.getType() == AlertChannelType.EMAIL)
                .forEach(c -> sendToEmail(c.getDestination(), incident));

        // 4. Dispatch to Slack
        if (setting != null && setting.isSlackEnabled() && setting.getSlackWebhookUrl() != null
                && !setting.getSlackWebhookUrl().isBlank()) {
            sendToSlack(setting.getSlackWebhookUrl(), incident);
        }

        // Dispatch to all monitor-specific SLACK configs
        configs.stream()
                .filter(c -> c.getType() == AlertChannelType.SLACK)
                .forEach(c -> sendToSlack(c.getDestination(), incident));
    }

    private void sendToEmail(String recipient, Incident incident) {
        log.debug("Dispatching EMAIL to {}", recipient);
        AlertDelivery delivery = deliveryService.createPending(incident, AlertChannelType.EMAIL, recipient);
        try {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                emailSender.sendRecoveryEmail(recipient, incident);
            } else {
                emailSender.sendIncidentEmail(recipient, incident);
            }
            deliveryService.markSuccess(delivery, "Email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
            deliveryService.markFailed(delivery, e.getMessage());
        }
    }

    private void sendToSlack(String webhookUrl, Incident incident) {
        log.debug("Dispatching SLACK to {}", webhookUrl);
        AlertDelivery delivery = deliveryService.createPending(incident, AlertChannelType.SLACK, "Webhook URL");
        try {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                slackSender.sendRecoveryMessage(webhookUrl, incident);
            } else {
                slackSender.sendIncidentMessage(webhookUrl, incident);
            }
            deliveryService.markSuccess(delivery, "Slack message sent");
        } catch (Exception e) {
            log.error("Failed to send slack notification: {}", e.getMessage());
            deliveryService.markFailed(delivery, e.getMessage());
        }
    }
}
