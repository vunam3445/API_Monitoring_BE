package com.example.demo.entities;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "subscriptions")
@Data
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    private LocalDateTime startDate;
    private LocalDateTime currentPeriodEnd;

    private String billingCycle; // MONTHLY, YEARLY
    private String status; // ACTIVE, EXPIRED, CANCELED
    private String paymentStatus; // PAID, PENDING, FAILED
}