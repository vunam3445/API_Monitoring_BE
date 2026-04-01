package com.example.demo.modules.paymentLogs.repositories;

import com.example.demo.modules.paymentLogs.entities.PaymentLogs;
import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentLogsRepository extends JpaRepository<PaymentLogs, UUID> {
    Optional<PaymentLogs> findByTransactionId(String transactionId);

    Page<PaymentLogs> findAllByUserId(UUID userId, Pageable pageable);

    Optional<PaymentLogs> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PaymentStatus status);
}
