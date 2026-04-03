package com.example.demo.modules.alert.controllers;

import com.example.demo.modules.alert.services.EmailSenderService;
import com.example.demo.modules.alert.services.SlackWebhookSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts/test")
@RequiredArgsConstructor
public class AlertTestController {

    private final EmailSenderService emailSender;
    private final SlackWebhookSenderService slackSender;

    @PostMapping("/email")
    public ResponseEntity<String> testEmail(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        if (to == null) return ResponseEntity.badRequest().body("to is required");
        try {
            emailSender.sendTestEmail(to);
            return ResponseEntity.ok("Test email sent to " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send test email: " + e.getMessage());
        }
    }

    @PostMapping("/slack")
    public ResponseEntity<String> testSlack(@RequestBody Map<String, String> request) {
        String url = request.get("webhookUrl");
        if (url == null) return ResponseEntity.badRequest().body("webhookUrl is required");
        try {
            slackSender.sendTestMessage(url);
            return ResponseEntity.ok("Test slack message sent");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send test slack message: " + e.getMessage());
        }
    }
}
