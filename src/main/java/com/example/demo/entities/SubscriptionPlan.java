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
    private Double price;
    private Integer maxMonitors; // Giới hạn số lượng API được phép tạo
    private Integer minInterval; // Tần suất tối thiểu (vd: Pro mới được 30s)
}