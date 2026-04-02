package com.example.demo.modules.alert.entities;

import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "alert_deliveries",
        indexes = {
                @Index(name = "idx_alert_delivery_incident", columnList = "incident_id"),
                @Index(name = "idx_alert_delivery_status", columnList = "status"),
                @Index(name = "idx_alert_delivery_sent_at", columnList = "sent_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Incident gốc
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    /**
     * Config đã dùng để gửi alert
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_config_id")
    private AlertConfig alertConfig;

    /**
     * Kênh thực tế dùng để gửi
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannelType channel;

    /**
     * Địa chỉ nhận thực tế
     */
    @Column(columnDefinition = "TEXT")
    private String destination;

    /**
     * PENDING / SENT / FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertDeliveryStatus status;

    /**
     * Nội dung đã gửi
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Lỗi khi gửi nếu có
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}