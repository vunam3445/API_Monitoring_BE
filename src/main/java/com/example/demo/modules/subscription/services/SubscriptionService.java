package com.example.demo.modules.subscription.services;

import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.BillingCycle;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;

    /**
     * Kích hoạt gói FREE cho người dùng (Không qua cổng thanh toán)
     */
    @Transactional
    public void subscribeFreePlan(User user) {
        SubscriptionPlan freePlan = planRepository.findByName("FREE")
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi hệ thống: Không tìm thấy cấu hình gói FREE."));

        // 1. Cập nhật thông tin gói trên User entity
        user.setSubscriptionPlan(freePlan);
        user.setPlanType("FREE");
        userRepository.save(user);

        // 2. Tìm subscription hiện tại hoặc tạo mới
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElse(new Subscription());

        subscription.setUser(user);
        subscription.setPlan(freePlan);
        subscription.setPlanName("FREE");
        subscription.setPlanPrice(BigDecimal.ZERO);
        subscription.setCurrency("VND");
        subscription.setMaxMonitors(freePlan.getMaxMonitors());
        subscription.setMinInterval(freePlan.getMinInterval());
        subscription.setStartDate(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusYears(10)); // Gói FREE hiệu lực lâu dài
        subscription.setBillingCycle(BillingCycle.FREE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentStatus(PaymentStatus.FREE);

        subscriptionRepository.save(subscription);
    }
}
