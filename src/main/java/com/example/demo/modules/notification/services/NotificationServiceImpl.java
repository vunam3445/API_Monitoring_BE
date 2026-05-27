package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.config.NotificationMQConfig;
import com.example.demo.modules.notification.dto.NotificationBroadcastEvent;
import com.example.demo.modules.notification.dto.NotificationResponse;
import com.example.demo.modules.notification.dto.SendNotificationRequest;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import com.example.demo.modules.notification.entities.Notification;
import com.example.demo.modules.notification.repositories.NotificationRepository;
import com.example.demo.modules.notification.repositories.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * PHA 1: Lưu bản ghi gốc vào DB, sau đó đẩy event vào RabbitMQ.
     * Trả về kết quả ngay lập tức mà không chờ đợi email hay web delivery.
     */
    @Override
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        // 1. Lưu thông báo gốc vào database
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .targetType(request.getTargetType())
                .targetValue(request.getTargetValue())
                .level(request.getLevel())
                .sendWeb(request.isSendWeb())
                .sendEmail(request.isSendEmail())
                .build();

        notification = notificationRepository.save(notification);
        log.info("[NotificationService] Đã lưu thông báo id={}, title='{}'", notification.getId(), notification.getTitle());

        // 2. Đẩy sự kiện vào RabbitMQ để Consumer xử lý bất đồng bộ
        NotificationBroadcastEvent event = new NotificationBroadcastEvent(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType().name(),
                notification.getTargetValue(),
                notification.getLevel().name(),
                notification.isSendWeb(),
                notification.isSendEmail()
        );

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(NotificationMQConfig.BROADCAST_QUEUE, event);
                        log.info("[NotificationService] Đã đẩy sự kiện vào queue sau khi Transaction commit thành công: {}", NotificationMQConfig.BROADCAST_QUEUE);
                    }
                }
            );
        } else {
            rabbitTemplate.convertAndSend(NotificationMQConfig.BROADCAST_QUEUE, event);
            log.info("[NotificationService] Không có Transaction hoạt động. Đã đẩy sự kiện vào queue lập tức: {}", NotificationMQConfig.BROADCAST_QUEUE);
        }

        return NotificationResponse.from(notification);
    }

    @Override
    public Page<NotificationResponse> getAdminHistory(Pageable pageable) {
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public Page<UserNotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        if (unreadOnly) {
            return userNotificationRepository
                    .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                    .map(UserNotificationResponse::from);
        }
        return userNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(UserNotificationResponse::from);
    }

    @Override
    public long countUnread(UUID userId) {
        return userNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userNotificationId, UUID userId) {
        userNotificationRepository.findByIdAndUserId(userNotificationId, userId)
                .ifPresent(un -> {
                    if (!un.isRead()) {
                        un.setRead(true);
                        un.setReadAt(LocalDateTime.now());
                        userNotificationRepository.save(un);
                    }
                });
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        int updated = userNotificationRepository.markAllAsRead(userId);
        log.info("[NotificationService] Đã đánh dấu {} thông báo đã đọc cho userId={}", updated, userId);
    }
}
