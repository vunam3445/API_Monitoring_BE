package com.example.demo.modules.paymentLogs.dto;

import com.example.demo.modules.paymentLogs.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLogsResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String planName;
    private String transactionId;
    private String paymentMethod;
    private String invoiceId;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
