package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;

public interface EmailSenderService {
    void sendIncidentEmail(String recipient, Incident incident);
    void sendRecoveryEmail(String recipient, Incident incident);
    void sendTestEmail(String recipient);
    void sendSubscriptionExpiryEmail(String recipient, String userName, String planName, String expiryDate);
}
