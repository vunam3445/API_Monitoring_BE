package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Data
public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String description;
    private Double price;
    private String currency = "USD";

    private Integer maxMonitors;
    private Integer minInterval;

    @Column(columnDefinition = "jsonb")
    private String features; // Lưu: {"custom_reports": true, "sms_alerts": false}

    private Boolean isActive = true;
}