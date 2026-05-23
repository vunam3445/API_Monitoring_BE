package com.example.demo.modules.subscription.repositories;

import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT SUM(s.planPrice) FROM Subscription s WHERE s.status = :status")
    BigDecimal sumPlanPriceByStatus(@Param("status") SubscriptionStatus status);

    long countByStatusAndCurrentPeriodEndBetween(SubscriptionStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s.plan.id, COUNT(s), SUM(s.planPrice) FROM Subscription s " +
           "WHERE s.status = :status " +
           "GROUP BY s.plan.id")
    List<Object[]> countAndSumByPlanAndStatus(@Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH s.plan " +
           "WHERE s.status = :status " +
           "AND s.currentPeriodEnd >= :startDate " +
           "AND s.currentPeriodEnd <= :endDate " +
           "AND s.plan.price > 0")
    List<Subscription> findActivePaidSubscriptionsExpiringBetween(
            @Param("status") SubscriptionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM Subscription s " +
           "JOIN FETCH s.user " +
           "WHERE s.status = :status " +
           "AND s.currentPeriodEnd < :now " +
           "AND s.plan.name <> 'FREE'")
    List<Subscription> findActivePaidSubscriptionsExpiringBefore(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);
}
