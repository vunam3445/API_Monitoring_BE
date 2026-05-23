package com.example.demo.modules.subscription.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.example.demo.modules.subscription.enums.BillingCycle;

@Entity
@Table(name = "subscription_plans")
@Data
// 1. Tự động chuyển lệnh DELETE thành lệnh UPDATE is_delete = true
@SQLDelete(sql = "UPDATE subscription_plans SET is_delete = true WHERE id = ?")
// 2. Tự động lọc các bản ghi đã xóa khỏi tất cả các câu lệnh SELECT (findById, findAll,...)
@SQLRestriction("is_delete = false")public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private BigDecimal price;
    private String currency = "VND";

    private Integer maxMonitors;
    private Integer minInterval;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @JdbcTypeCode(SqlTypes.JSON) // Quan trọng nhất: Giúp Hibernate hiểu đây là JSON
    @Column(columnDefinition = "jsonb")
    private String features;

    private Boolean isActive = true;
    @Column(name = "is_delete")
    private Boolean isDelete = false;
}
