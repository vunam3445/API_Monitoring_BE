package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;

public interface SlackWebhookSenderService {
    void sendIncidentMessage(String webhookUrl, Incident incident);
    void sendRecoveryMessage(String webhookUrl, Incident incident);
    void sendTestMessage(String webhookUrl);
}
