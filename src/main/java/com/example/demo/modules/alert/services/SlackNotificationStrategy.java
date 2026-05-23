package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Strategy cho kênh SLACK.
 * Delegate toàn bộ logic gửi webhook sang SlackWebhookSenderService.
 */
@Service
@RequiredArgsConstructor
public class SlackNotificationStrategy implements NotificationStrategy {

    private final SlackWebhookSenderService slackWebhookSenderService;

    @Override
    public AlertChannelType supportedChannel() {
        return AlertChannelType.SLACK;
    }

    @Override
    public void sendIncident(String destination, Incident incident) {
        slackWebhookSenderService.sendIncidentMessage(destination, incident);
    }

    @Override
    public void sendRecovery(String destination, Incident incident) {
        slackWebhookSenderService.sendRecoveryMessage(destination, incident);
    }
}
