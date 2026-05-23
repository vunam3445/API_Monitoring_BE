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

    @Override
    public void sendSubscriptionExpiryEmail(String recipient, String userName, String planName, String expiryDate) {
        String subject = String.format("[API Monitoring] Gia hạn gói dịch vụ %s sắp hết hạn", planName);
        String content = buildSubscriptionExpiryHtmlContent(userName, planName, expiryDate);
        sendHtmlEmail(recipient, subject, content);
    }

    private String buildSubscriptionExpiryHtmlContent(String userName, String planName, String expiryDate) {
        return String.format(
            "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; padding: 40px 10px; color: #1e293b;\">" +
            "    <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;\">" +
            "        <div style=\"background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%); padding: 35px 20px; text-align: center;\">" +
            "            <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;\">API Monitoring</h1>" +
            "            <p style=\"color: #e0e7ff; margin: 5px 0 0 0; font-size: 14px;\">Hệ thống giám sát hiệu năng API thông minh</p>" +
            "        </div>" +
            "        <div style=\"padding: 40px 30px;\">" +
            "            <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 700;\">Xin chào, %s!</h2>" +
            "            <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">" +
            "                Chúng tôi xin thông báo gói dịch vụ trả phí <strong>%s</strong> của bạn trên hệ thống <strong>API Monitoring</strong> sẽ hết hạn vào ngày <span style=\"color: #ef4444; font-weight: 700;\">%s</span> (3 ngày nữa)." +
            "            </p>" +
            "            <div style=\"background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px; margin: 25px 0;\">" +
            "                <h4 style=\"color: #b45309; margin: 0 0 8px 0; font-size: 15px; font-weight: 700;\">⚠️ Lưu ý quan trọng</h4>" +
            "                <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #78350f;\">" +
            "                    Sau ngày hết hạn, tài khoản của bạn sẽ tự động chuyển về gói <strong>FREE</strong>. Các giới hạn như số lượng API giám sát, thời gian chu kỳ quét sẽ bị thu hẹp đáng kể, gây gián đoạn việc giám sát hệ thống của bạn." +
            "                </p>" +
            "            </div>" +
            "            <p style=\"font-size: 15px; line-height: 1.6; color: #475569; text-align: center; margin-top: 30px;\">" +
            "                Hãy gia hạn hoặc nâng cấp ngay hôm nay để duy trì kết nối giám sát 24/7 không bị ngắt quãng!" +
            "            </p>" +
            "            <div style=\"text-align: center; margin: 35px 0;\">" +
            "                <a href=\"http://localhost:3000/admin/billing\" style=\"background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 14px 35px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2);\">" +
            "                    Gia Hạn Gói Ngay" +
            "                </a>" +
            "            </div>" +
            "        </div>" +
            "        <div style=\"background-color: #f1f5f9; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;\">" +
            "            <p style=\"margin: 0 0 5px 0;\">Đây là email tự động từ hệ thống API Monitoring, vui lòng không trả lời trực tiếp email này.</p>" +
            "            <p style=\"margin: 0;\">&copy; 2026 API Monitoring Team. All rights reserved.</p>" +
            "        </div>" +
            "    </div>" +
            "</div>",
            userName, planName, expiryDate
        );
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
