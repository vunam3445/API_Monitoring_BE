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

    @RabbitListener(queues = NotificationMQConfig.BROADCAST_QUEUE)
    @Transactional
    public void consume(NotificationBroadcastEvent event) {
        log.info("[NotificationConsumer] Nhận sự kiện phát thông báo id={}, target={}/{}",
                event.notificationId(), event.targetType(), event.targetValue());

        try {
            // 1. Lấy notification entity từ DB
            Notification notification = notificationRepository.findById(event.notificationId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Notification not found: " + event.notificationId()));

            // 2. Xác định danh sách người dùng nhận thông báo
            List<User> recipients = resolveRecipients(event);
            log.info("[NotificationConsumer] Số người nhận: {}", recipients.size());

            // 3. Kênh Web: Tạo hàng loạt bản ghi user_notifications và đẩy SSE thời gian thực
            if (event.sendWeb()) {
                List<UserNotification> records = recipients.stream()
                        .map(user -> UserNotification.builder()
                                .user(user)
                                .notification(notification)
                                .build())
                        .collect(Collectors.toList());
                List<UserNotification> savedRecords = userNotificationRepository.saveAll(records);
                log.info("[NotificationConsumer] Đã tạo {} bản ghi web notification", savedRecords.size());

                // Gửi đẩy Server-Sent Events thời gian thực cho từng User nhận đang Online
                for (UserNotification record : savedRecords) {
                    try {
                        UserNotificationResponse responseDto = UserNotificationResponse.from(record);
                        notificationSseService.sendNotification(record.getUser().getId(), responseDto);
                    } catch (Exception sseEx) {
                        log.warn("[NotificationConsumer] Lỗi gửi SSE tới userId={}: {}", 
                                record.getUser().getId(), sseEx.getMessage());
                    }
                }
            }

            // 4. Kênh Email: Gửi email từng người nhận
            if (event.sendEmail()) {
                for (User user : recipients) {
                    try {
                        emailSenderService.sendNotificationEmail(
                                user.getEmail(),
                                event.title(),
                                event.content(),
                                event.level()
                        );
                    } catch (Exception e) {
                        log.error("[NotificationConsumer] Lỗi gửi email tới {}: {}", user.getEmail(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("[NotificationConsumer] Lỗi xử lý sự kiện id={}: {}", event.notificationId(), e.getMessage());
            // Throw để RabbitMQ giữ message và thử lại theo cơ chế retry
            throw new RuntimeException("Failed to process notification broadcast event", e);
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
