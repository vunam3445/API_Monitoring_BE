package com.example.demo.modules.revenue.services;

import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import com.example.demo.modules.paymentLogs.repositories.PaymentLogsRepository;
import com.example.demo.modules.revenue.dto.*;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.user.enums.UserRole;
import com.example.demo.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService implements IRevenueService {

    private final PaymentLogsRepository paymentLogsRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;

    @Override
    public RevenueStatsDTO getRevenueStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        // 1. Total Revenue
        BigDecimal totalRevenue = paymentLogsRepository.sumAmountByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, LocalDateTime.of(2000, 1, 1, 0, 0));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // 2. Revenue Growth (Last 30 days vs 30-60 days ago)
        BigDecimal revLast30 = paymentLogsRepository.sumAmountByStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, thirtyDaysAgo, now);
        BigDecimal revPrev30 = paymentLogsRepository.sumAmountByStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, sixtyDaysAgo, thirtyDaysAgo);
        Double revenueGrowth = calculateGrowth(revLast30, revPrev30);

        // 3. MRR (Sum of all active subscription monthly prices)
        BigDecimal mrr = subscriptionRepository.sumPlanPriceByStatus(SubscriptionStatus.ACTIVE);
        if (mrr == null) mrr = BigDecimal.ZERO;
        
        // Mock MRR Growth for demo
        Double mrrGrowth = 8.2;

        // 4. Active Subscriptions
        long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        
        // Mock Subs Growth for demo
        Double subsGrowth = 15.0;

        // 5. Expiring Soon (Next 7 days)
        long expiringSoon = subscriptionRepository.countByStatusAndCurrentPeriodEndBetween(
                SubscriptionStatus.ACTIVE, now, now.plusDays(7));

        // 6. ARPU (Average Revenue Per User)
        long totalUsers = userRepository.count();
        Double arpu = totalUsers == 0 ? 0.0 : revLast30.doubleValue() / totalUsers;

        // 7. LTV (Life Time Value) - Simplified mock
        Double ltv = arpu * 12; // Assuming 12 months average lifetime

        return RevenueStatsDTO.builder()
                .totalRevenue(totalRevenue)
                .revenueGrowth(revenueGrowth)
                .mrr(mrr)
                .mrrGrowth(mrrGrowth)
                .activeSubscriptions(activeSubs)
                .subsGrowth(subsGrowth)
                .expiringSoon(expiringSoon)
                .arpu(Math.round(arpu * 10.0) / 10.0)
                .ltv(Math.round(ltv * 10.0) / 10.0)
                .build();
    }

    @Override
    public RevenueChartDTO getRevenueCharts(String period) {
        // Mocking daily revenue for the last 15 days for demo
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 14; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            labels.add(date.format(formatter));
            
            BigDecimal dayRev = paymentLogsRepository.sumAmountByStatusAndCreatedAtBetween(
                    PaymentStatus.SUCCESS, date.toLocalDate().atStartOfDay(), date.toLocalDate().atTime(23, 59, 59));
            data.add(dayRev != null ? dayRev.doubleValue() : 0.0);
        }

        return RevenueChartDTO.builder()
                .labels(labels)
                .datasets(List.of(
                        RevenueChartDTO.DatasetDTO.builder()
                                .label("Revenue")
                                .data(data)
                                .build()
                ))
                .build();
    }

    @Override
    public SubscriptionAnalyticsDTO getSubscriptionAnalytics() {
        List<Object[]> planStats = userRepository.countUsersByPlan(UserRole.ADMIN);
        
        long free = 0;
        long paid = 0;
        
        for (Object[] stat : planStats) {
            String plan = (String) stat[0];
            Long count = (Long) stat[1];
            if ("FREE".equalsIgnoreCase(plan)) {
                free += count;
            } else {
                paid += count;
            }
        }

        return SubscriptionAnalyticsDTO.builder()
                .usersComparison(SubscriptionAnalyticsDTO.UsersComparisonDTO.builder()
                        .free(free)
                        .paid(paid)
                        .build())
                .upgradeTrends(SubscriptionAnalyticsDTO.UpgradeTrendsDTO.builder()
                        .count(paid) // Using total paid as a proxy for demo
                        .growth(18.4)
                        .build())
                .churnMetrics(SubscriptionAnalyticsDTO.ChurnMetricsDTO.builder()
                        .rate(2.1)
                        .status("Good")
                        .build())
                .build();
    }

    @Override
    public List<PlanBreakdownDTO> getPlanBreakdown() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        List<Object[]> subStats = subscriptionRepository.countAndSumByPlanAndStatus(SubscriptionStatus.ACTIVE);
        Map<UUID, Object[]> statsMap = subStats.stream()
                .collect(Collectors.toMap(s -> (UUID) s[0], s -> s));

        return plans.stream().map(plan -> {
            Object[] stats = statsMap.get(plan.getId());
            Long activeCount = stats != null ? (Long) stats[1] : 0L;
            BigDecimal monthlyRev = stats != null ? (BigDecimal) stats[2] : BigDecimal.ZERO;

            return PlanBreakdownDTO.builder()
                    .id(plan.getId().toString())
                    .name(plan.getName())
                    .activeSubscribers(activeCount)
                    .monthlyRevenue(monthlyRev)
                    .churned30d(0L) // Mock
                    .retention(95.0) // Mock
                    .growth(5.0) // Mock
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public Page<RecentTransactionDTO> getRecentTransactions(Pageable pageable) {
        return paymentLogsRepository.findAll(pageable).map(log -> RecentTransactionDTO.builder()
                .id(log.getId())
                .userName(log.getUser().getFullName() != null ? log.getUser().getFullName() : log.getUser().getEmail())
                .userEmail(log.getUser().getEmail())
                .amount(log.getAmount())
                .plan(log.getPlanName())
                .date(log.getCreatedAt())
                .status(log.getStatus().name())
                .build());
    }

    private Double calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (current == null) current = BigDecimal.ZERO;
        
        BigDecimal growth = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return growth.doubleValue();
    }
}
