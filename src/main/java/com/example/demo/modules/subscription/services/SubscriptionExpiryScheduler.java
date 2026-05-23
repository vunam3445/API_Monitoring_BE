package com.example.demo.modules.subscription.services;

import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ISubscriptionService subscriptionService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Scheduled(cron = "0 0 7 * * ?", zone = "Asia/Ho_Chi_Minh") // 7h sáng hàng ngày (Giờ Hà Nội)
    public void scanAndNotifyExpiringSubscriptions() {
        log.info("[SubscriptionExpiryScheduler] Khởi chạy quét gói dịch vụ sắp hết hạn sau 3 ngày...");

        LocalDateTime startDate = LocalDateTime.now().plusDays(3).with(LocalTime.MIN);
        LocalDateTime endDate = LocalDateTime.now().plusDays(3).with(LocalTime.MAX);

        List<Subscription> expiringSubs = subscriptionRepository
                .findActivePaidSubscriptionsExpiringBetween(SubscriptionStatus.ACTIVE, startDate, endDate);

        log.info("[SubscriptionExpiryScheduler] Tìm thấy {} subscriptions trả phí sắp hết hạn vào ngày {}", 
                expiringSubs.size(), startDate.format(DATE_FORMATTER));

        for (Subscription sub : expiringSubs) {
            if (sub.getUser() == null || sub.getUser().getEmail() == null) continue;

            SubscriptionExpiryEvent event = new SubscriptionExpiryEvent(
                    sub.getId().toString(),
                    sub.getUser().getEmail(),
                    sub.getUser().getFullName() != null ? sub.getUser().getFullName() : "Khách hàng",
                    sub.getPlanName() != null ? sub.getPlanName() : sub.getPlan().getName(),
                    sub.getCurrentPeriodEnd().format(DATE_FORMATTER)
              );

            rabbitTemplate.convertAndSend(SubscriptionExpiryMQConfig.EXPIRY_QUEUE, event);
            log.info("[SubscriptionExpiryScheduler] Đã đẩy sự kiện hết hạn vào RabbitMQ cho user: {}", sub.getUser().getEmail());
        }
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Ho_Chi_Minh") // Quét mỗi giờ một lần (Giờ Hà Nội)
    @Transactional
    public void scanAndDowngradeExpiredSubscriptions() {
        log.info("[SubscriptionExpiryScheduler] Khởi chạy quét và hạ cấp các gói dịch vụ đã hết hạn...");

        List<Subscription> expiredSubs = subscriptionRepository
                .findActivePaidSubscriptionsExpiringBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());

        log.info("[SubscriptionExpiryScheduler] Tìm thấy {} gói dịch vụ đã hết hạn cần hạ cấp.", expiredSubs.size());

        for (Subscription sub : expiredSubs) {
            try {
                if (sub.getUser() == null) continue;
                
                log.info("[SubscriptionExpiryScheduler] Đang hạ cấp gói cho user: {} (Gói cũ: {})", 
                        sub.getUser().getEmail(), sub.getPlanName());

                // 1. Chuyển trạng thái gói cũ thành EXPIRED
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);

                // 2. Đăng ký tự động gói FREE mới cho người dùng
                subscriptionService.subscribeFreePlan(sub.getUser());

                log.info("[SubscriptionExpiryScheduler] Hạ cấp thành công user: {} về gói FREE.", sub.getUser().getEmail());
            } catch (Exception e) {
                log.error("[SubscriptionExpiryScheduler] Lỗi khi hạ cấp gói cho subscription ID {}: {}", 
                        sub.getId(), e.getMessage());
            }
        }
    }
}
