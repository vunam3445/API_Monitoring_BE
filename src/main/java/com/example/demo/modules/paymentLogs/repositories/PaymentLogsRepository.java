package com.example.demo.modules.paymentLogs.repositories;

import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentLogsRepository extends JpaRepository<PaymentLogs, UUID> {
    Optional<PaymentLogs> findByTransactionId(String transactionId);

    Page<PaymentLogs> findAllByUserId(UUID userId, Pageable pageable);

    Optional<PaymentLogs> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM PaymentLogs p WHERE p.status = :status AND p.createdAt >= :since")
    BigDecimal sumAmountByStatusAndCreatedAtAfter(
            @Param("status") PaymentStatus status,
            @Param("since") LocalDateTime since);

    @Query("SELECT SUM(p.amount) FROM PaymentLogs p WHERE p.status = :status AND p.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByStatusAndCreatedAtBetween(
            @Param("status") PaymentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT p FROM PaymentLogs p " +
           "JOIN p.user u " +
           "WHERE p.status = :status AND p.createdAt >= :since " +
           "ORDER BY p.createdAt DESC")
    List<PaymentLogs> findAllByStatusAndCreatedAtAfter(
            @Param("status") PaymentStatus status,
            @Param("since") LocalDateTime since);
}
