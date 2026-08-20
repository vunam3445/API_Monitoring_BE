package com.example.demo.modules.notification.services;

import com.example.demo.modules.alert.services.EmailSenderService;
import com.example.demo.modules.notification.config.NotificationMQConfig;
import com.example.demo.modules.notification.dto.NotificationBroadcastEvent;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import com.example.demo.modules.notification.entities.Notification;
import com.example.demo.modules.notification.entities.UserNotification;
import com.example.demo.modules.notification.repositories.NotificationRepository;
import com.example.demo.modules.notification.repositories.UserNotificationRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.entities.UserSetting;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import com.example.demo.modules.alert.services.SlackWebhookSenderService;
import com.example.demo.modules.notification.enums.NotificationLevel;
import com.example.demo.modules.notification.enums.TargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumer bất đồng bộ xử lý phân phối thông báo đa kênh.
 *
 * Trách nhiệm:
 * - Kênh Web: Tạo bản ghi UserNotification cho từng người nhận (DB) và đẩy SSE thời gian thực.
 * - Kênh Email: Gửi email HTML đến từng người nhận qua SMTP Brevo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationBroadcastConsumer {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final NotificationSseService notificationSseService;
    private final com.example.demo.modules.system.services.ActiveWorkerRegistry activeWorkerRegistry;
    private final UserSettingRepository userSettingRepository;
    private final SlackWebhookSenderService slackWebhookSenderService;

    @RabbitListener(
            queues = NotificationMQConfig.BROADCAST_QUEUE,
            concurrency = "${app.rabbitmq.concurrency.broadcast}"
    )
    @Transactional
    public void consume(NotificationBroadcastEvent event) {
        log.info("[NotificationConsumer] Nhận sự kiện phát thông báo id={}, target={}/{}",
                event.notificationId(), event.targetType(), event.targetValue());
        activeWorkerRegistry.increment();
        try {
            // 1. Lấy notification entity từ DB
            Notification notification = notificationRepository.findById(event.notificationId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Notification not found: " + event.notificationId()));

            // 2. Xác định danh sách người dùng nhận thông báo
            List<User> recipients = resolveRecipients(event);
            log.info("[NotificationConsumer] Số người nhận: {}", recipients.size());

            // 3. Xử lý gửi thông báo cho từng người nhận dựa trên cấu hình UserSetting
            for (User user : recipients) {
                UserSetting setting = userSettingRepository.findById(user.getId()).orElse(null);
                boolean emailAlertsEnabled = setting == null || setting.isEmailAlertsEnabled();
                boolean slackEnabled = setting != null && setting.isSlackEnabled();
                String slackWebhookUrl = setting != null ? setting.getSlackWebhookUrl() : null;

                // A. Kênh Web (SSE + DB UserNotification)
                if (event.sendWeb()) {
                    try {
                        UserNotification record = UserNotification.builder()
                                .user(user)
                                .notification(notification)
                                .build();
                        record = userNotificationRepository.save(record);

                        UserNotificationResponse responseDto = UserNotificationResponse.from(record);
                        notificationSseService.sendNotification(user.getId(), responseDto);
                    } catch (Exception sseEx) {
                        log.warn("[NotificationConsumer] Lỗi gửi SSE tới userId={}: {}", 
                                user.getId(), sseEx.getMessage());
                    }
                }

                // Xác định địa chỉ email nhận thông báo: Ưu tiên alertEmail từ UserSetting, nếu trống thì dùng user.getEmail()
                String recipientEmail = (setting != null && setting.getAlertEmail() != null && !setting.getAlertEmail().isBlank())
                        ? setting.getAlertEmail()
                        : user.getEmail();

                // B. Kênh Email: Gửi email chỉ nếu là cấp SYSTEM hoặc user bật cài đặt nhận email
                if (event.sendEmail()) {
                    if ("SYSTEM".equalsIgnoreCase(event.level()) || emailAlertsEnabled) {
                        try {
                            emailSenderService.sendNotificationEmail(
                                    recipientEmail,
                                    event.title(),
                                    event.content(),
                                    event.level()
                            );
                        } catch (Exception e) {
                            log.error("[NotificationConsumer] Lỗi gửi email tới {}: {}", recipientEmail, e.getMessage());
                        }
                    }
                }

                // C. Kênh Slack: Gửi tin nhắn qua Webhook Slack
                if (slackEnabled) {
                    if (slackWebhookUrl != null && !slackWebhookUrl.isBlank()) {
                        try {
                            String slackMsg = String.format("*🔔 [HỆ THỐNG] THÔNG BÁO TỪ QUẢN TRỊ VIÊN*%n*Tiêu đề:* %s%n*Nội dung:* %s%n*Mức độ:* %s", 
                                    event.title(), event.content(), event.level());
                            slackWebhookSenderService.sendSlackMessage(slackWebhookUrl, slackMsg);
                        } catch (Exception e) {
                            log.error("[NotificationConsumer] Lỗi gửi Slack tới user {}: {}", user.getEmail(), e.getMessage());
                        }
                    } else {
                        // PHƯƠNG ÁN B: Bật Slack nhưng thiếu Webhook URL -> Gửi cảnh báo nhắc nhở qua Email và Web/SSE
                        try {
                            String warnTitle = "⚠️ [Hệ thống] Yêu cầu cấu hình Slack Webhook URL";
                            String warnContent = "Bạn đã kích hoạt nhận thông báo qua Slack nhưng chưa cấu hình Webhook URL. Vui lòng cập nhật trong phần 'Cài đặt tài khoản' để nhận được các cảnh báo quan trọng.";
                            
                            // Gửi email cảnh báo
                            emailSenderService.sendNotificationEmail(recipientEmail, warnTitle, warnContent, "WARNING");
                            
                            // Gửi Web/SSE cảnh báo
                            if (event.sendWeb()) {
                                Notification warningNotif = notificationRepository.save(Notification.builder()
                                        .title(warnTitle)
                                        .content(warnContent)
                                        .targetType(TargetType.SINGLE)
                                        .targetValue(recipientEmail)
                                        .level(NotificationLevel.WARNING)
                                        .sendWeb(true)
                                        .sendEmail(false)
                                        .build());
                                UserNotification warningRecord = userNotificationRepository.save(UserNotification.builder()
                                        .user(user)
                                        .notification(warningNotif)
                                        .build());
                                notificationSseService.sendNotification(user.getId(), UserNotificationResponse.from(warningRecord));
                            }
                            log.info("[NotificationConsumer] Đã gửi cảnh báo thiếu Slack Webhook cho userId={}", user.getId());
                        } catch (Exception ex) {
                            log.error("[NotificationConsumer] Lỗi gửi cảnh báo thiếu Slack Webhook tới {}: {}", recipientEmail, ex.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("[NotificationConsumer] Lỗi xử lý sự kiện id={}: {}", event.notificationId(), e.getMessage());
            // Throw để RabbitMQ giữ message và thử lại theo cơ chế retry
            throw new RuntimeException("Failed to process notification broadcast event", e);
        } finally {
            activeWorkerRegistry.decrement();
        }
    }

    /**
     * Phân giải danh sách người nhận dựa trên targetType.
     */
    private List<User> resolveRecipients(NotificationBroadcastEvent event) {
        return switch (event.targetType()) {
            case "ALL" -> userRepository.findAll().stream()
                    .filter(u -> !"ADMIN".equals(u.getRole().name()))
                    .collect(Collectors.toList());

            case "PLAN" -> userRepository.findAll().stream()
                    .filter(u -> event.targetValue() != null
                            && event.targetValue().equalsIgnoreCase(u.getPlanType()))
                    .collect(Collectors.toList());

            case "SINGLE" -> userRepository.findByEmail(event.targetValue())
                    .map(List::of)
                    .orElse(List.of());

            default -> {
                log.warn("[NotificationConsumer] Loại target không hợp lệ: {}", event.targetType());
                yield List.of();
            }
        };
    }
}
