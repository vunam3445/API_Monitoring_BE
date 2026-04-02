package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackWebhookSenderServiceImpl implements SlackWebhookSenderService {

    private final WebClient.Builder webClientBuilder;

    @Override
    public void sendIncidentMessage(String webhookUrl, Incident incident) {
        String message = String.format("*[%s] API Alert!*%n*Monitor:* %s%n*Type:* %s%n*Message:* %s",
                incident.getSeverity(),
                incident.getMonitor().getName(),
                incident.getType(),
                incident.getMessage());
        
        postToWebhook(webhookUrl, message);
    }

    @Override
    public void sendRecoveryMessage(String webhookUrl, Incident incident) {
        String message = String.format("*[RESOLVED] API Recovered*%n*Monitor:* %s%n*URL:* %s",
                incident.getMonitor().getName(),
                incident.getMonitor().getUrl());
        
        postToWebhook(webhookUrl, message);
    }

    @Override
    public void sendTestMessage(String webhookUrl) {
        postToWebhook(webhookUrl, "This is a test notification from API Monitoring tool.");
    }

    private void postToWebhook(String url, String text) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Sent slack message successfully");
        } catch (Exception e) {
            log.error("Failed to send slack message to {}: {}", url, e.getMessage());
            throw new RuntimeException("Slack delivery failed: " + e.getMessage());
        }
    }
}
