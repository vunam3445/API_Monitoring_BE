package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_logs")
@Data
public class PaymentLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    private Double amount;
    private String currency;
    private String transactionId;
    private String paymentMethod; // STRIPE, PAYPAL, VNPAY
    private String invoiceId;
    private String status; // SUCCESS, FAILED
    private LocalDateTime createdAt = LocalDateTime.now();
}