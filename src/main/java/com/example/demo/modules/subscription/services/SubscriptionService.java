package com.example.demo.modules.subscription.services;

import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.common.exceptions.SubscriptionNotFoundException;
import com.example.demo.common.exceptions.UserNotFoundException;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import com.example.demo.modules.subscription.dto.ManualRenewalRequest;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.BillingCycle;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.user.entities.User;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.dashboard.services.DashboardCacheService;
import com.example.demo.common.cache.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements ISubscriptionService {

        private final SubscriptionRepository subscriptionRepository;
        private final SubscriptionPlanRepository planRepository;
        private final UserRepository userRepository;
        private final DashboardCacheService dashboardCacheService;
        private final ICacheService cacheService;

        /**
         * Kích hoạt gói FREE cho người dùng (Không qua cổng thanh toán)
         */
        @Transactional
        @Override
        public void subscribeFreePlan(User user) {
                SubscriptionPlan freePlan = planRepository.findByName("FREE")
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Lỗi hệ thống: Không tìm thấy cấu hình gói FREE."));

                // 1. Cập nhật thông tin gói trên User entity
                user.setSubscriptionPlan(freePlan);
                user.setPlanType("FREE");
                userRepository.save(user);

                // 2. Tìm subscription hiện tại đang ACTIVE hoặc tạo mới
                Subscription subscription = subscriptionRepository
                                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
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
                dashboardCacheService.clearUserDashboardCache(user.getId());
        }

        @Override
        public void updatePlanByAdmin(UUID userId, UUID planId) {
                // 1. Kiểm tra tồn tại
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user!"));
                SubscriptionPlan subscriptionPlan = planRepository.findById(planId)
                                .orElseThrow(() -> new SubscriptionNotFoundException("Không tìm thấy plan!"));
                user.setPlanType(subscriptionPlan.getName());
                user.setSubscriptionPlan(subscriptionPlan);
                userRepository.save(user);
                // sử lý subsccription

                // 3. Đánh dấu subscription hiện tại (ACTIVE) thành EXPIRED và ghi thời gian kết
                // thúc
                Subscription oldSubscription = subscriptionRepository
                                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> new SubscriptionNotFoundException(
                                                "Không tìm thấy subscription ACTIVE của user!"));
                oldSubscription.setStatus(SubscriptionStatus.EXPIRED);
                oldSubscription.setCurrentPeriodEnd(LocalDateTime.now());
                subscriptionRepository.save(oldSubscription);

                // tạo subscription mới cho user
                // 4. Tạo subscription mới cho plan mới (lưu lịch sử)
                Subscription newSubscription = new Subscription();
                newSubscription.setUser(user);
                newSubscription.setPlan(subscriptionPlan);
                newSubscription.setPlanName(subscriptionPlan.getName());
                newSubscription.setPlanPrice(subscriptionPlan.getPrice());
                newSubscription.setCurrency(
                                subscriptionPlan.getCurrency() != null ? subscriptionPlan.getCurrency() : "VND");
                newSubscription.setMaxMonitors(subscriptionPlan.getMaxMonitors());
                newSubscription.setMinInterval(subscriptionPlan.getMinInterval());
                newSubscription.setStartDate(LocalDateTime.now());
                // Giả sử thời gian hiệu lực dựa trên chu kỳ billing (1 năm cho MONTHLY, 1 tháng
                // cho MONTHLY nếu có logic), ở đây tạm dùng 1 năm
                newSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusYears(1));
                newSubscription.setBillingCycle(subscriptionPlan.getBillingCycle());
                newSubscription.setStatus(SubscriptionStatus.ACTIVE);
                // Do admin cấp, coi thanh toán đã thành công
                newSubscription.setPaymentStatus(PaymentStatus.SUCCESS);
                subscriptionRepository.save(newSubscription);
                // Xóa cache người dùng và admin
                dashboardCacheService.clearUserDashboardCache(user.getId());
                cacheService.evictByPrefix("api-monitoring:admin:users:list::");
                cacheService.evictByPrefix("api-monitoring:admin:users:stats");

        }

    @Override
    public Boolean renewManual(ManualRenewalRequest request) {
        return null;
    }

}
