package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendIncidentEmail(String recipient, Incident incident) {
        String subject = String.format("[%s] %s is %s", 
                incident.getSeverity(), 
                incident.getMonitor().getName(), 
                incident.getType());
                
        String content = buildHtmlContent(incident);
        sendHtmlEmail(recipient, subject, content);
    }

    @Override
    public void sendRecoveryEmail(String recipient, Incident incident) {
        String subject = String.format("[RESOLVED] %s recovered", incident.getMonitor().getName());
        String content = buildRecoveryHtmlContent(incident);
        sendHtmlEmail(recipient, subject, content);
    }

    @Override
    public void sendTestEmail(String recipient) {
        sendHtmlEmail(recipient, "Test Notification", "<h1>Test</h1><p>Your API Monitoring alert system is working.</p>");
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("Sending email FROM {} TO {}", fromEmail, to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("SMTP Error: Failed to send email from {} to {}. Reason: {}", fromEmail, to, e.getMessage());
            // In Brevo, if you get 550 or unauthorized, it's often the domain/sender and not authentication login.
            throw new RuntimeException("Email delivery failed: " + e.getMessage());
        }
    }

    private String buildHtmlContent(Incident incident) {
        return String.format(
            "<div style='font-family: Arial, sans-serif;'>" +
            "<h2>Alert: %s</h2>" +
            "<p><b>Monitor:</b> %s</p>" +
            "<p><b>Endpoint:</b> %s</p>" +
            "<p><b>Type:</b> %s</p>" +
            "<p><b>Severity:</b> <span style='color: %s'>%s</span></p>" +
            "<p><b>Message:</b> %s</p>" +
            "<p><b>Time:</b> %s</p>" +
            "</div>",
            incident.getTitle(),
            incident.getMonitor().getName(),
            incident.getMonitor().getUrl(),
            incident.getType(),
            incident.getSeverity().toString().equals("CRITICAL") ? "red" : "orange",
            incident.getSeverity(),
            incident.getMessage(),
            incident.getTriggeredAt().format(formatter)
        );
    }

    private String buildRecoveryHtmlContent(Incident incident) {
        return String.format(
            "<div style='font-family: Arial, sans-serif; color: green;'>" +
            "<h2>Resolved: %s Recovery</h2>" +
            "<p><b>Monitor:</b> %s</p>" +
            "<p><b>Endpoint:</b> %s</p>" +
            "<p><b>Recovered At:</b> %s</p>" +
            "</div>",
            incident.getMonitor().getName(),
            incident.getMonitor().getName(),
            incident.getMonitor().getUrl(),
            incident.getResolvedAt() != null ? incident.getResolvedAt().format(formatter) : "N/A"
        );
    }
}
