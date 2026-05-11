package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Strategy cho kênh EMAIL.
 * Delegate toàn bộ logic gửi mail sang EmailSenderService.
 */
@Service
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationStrategy {

    private final EmailSenderService emailSenderService;

    @Override
    public AlertChannelType supportedChannel() {
        return AlertChannelType.EMAIL;
    }

    @Override
    public void sendIncident(String destination, Incident incident) {
        emailSenderService.sendIncidentEmail(destination, incident);
    }

    @Override
    public void sendRecovery(String destination, Incident incident) {
        emailSenderService.sendRecoveryEmail(destination, incident);
    }
}
