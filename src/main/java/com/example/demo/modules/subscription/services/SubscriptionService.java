package com.example.demo.modules.subscription.services;

import com.example.demo.common.exceptions.*;
import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
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
import com.example.demo.modules.paymentLogs.repositories.PaymentLogsRepository;
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
        private final PaymentLogsRepository paymentLogsRepository;

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
                if (subscriptionPlan.getBillingCycle() == BillingCycle.MONTHLY) {
                        newSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
                } else if (subscriptionPlan.getBillingCycle() == BillingCycle.YEARLY) {
                        newSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusYears(1));
                }
                if(subscriptionPlan.getBillingCycle() == BillingCycle.FREE) {
                        newSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusYears(10));
                }
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
        @Transactional
        public Boolean renewManual(UUID userId, ManualRenewalRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user!"));
                Subscription subscription = subscriptionRepository
                                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> new UserNotSubscriptionActive("User chưa có gói cước nào!"));
                if (subscription.getCurrentPeriodEnd().isBefore(LocalDateTime.now())) {
                        throw new SubscriptionExpiredException("Gói cước đã hết hạn!");
                }

                switch (request.getType()) {
                        case "day":
                                subscription.setCurrentPeriodEnd(
                                                subscription.getCurrentPeriodEnd().plusDays(request.getTime()));
                                break;
                        case "month":
                                subscription.setCurrentPeriodEnd(
                                                subscription.getCurrentPeriodEnd().plusMonths(request.getTime()));
                                break;
                        case "year":
                                subscription.setCurrentPeriodEnd(
                                                subscription.getCurrentPeriodEnd().plusYears(request.getTime()));
                                break;
                        default:
                                break;
                }

                subscriptionRepository.save(subscription);
                PaymentLogs paymentLogs = new PaymentLogs();
                paymentLogs.setUser(user);
                paymentLogs.setSubscription(subscription);
                paymentLogs.setAmount(request.getAmount());
                paymentLogs.setPlanName(subscription.getPlanName());
                paymentLogs.setInvoiceId(null);
                paymentLogs.setCurrency(subscription.getCurrency());
                paymentLogs.setStatus(PaymentStatus.SUCCESS);
                paymentLogs.setPaymentMethod("MANUAL");
                paymentLogs.setTransactionId(UUID.randomUUID().toString());
                paymentLogs.setNotes(request.getNote());
                paymentLogs.setCreatedAt(LocalDateTime.now());
                paymentLogsRepository.save(paymentLogs);

                // 3. Xóa cache dashboard để cập nhật thông tin mới
                dashboardCacheService.clearUserDashboardCache(userId);

                return true;
        }

}
