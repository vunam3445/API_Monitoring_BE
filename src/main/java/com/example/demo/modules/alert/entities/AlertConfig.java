package com.example.demo.modules.alert.entities;

import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.monitor.entities.Monitor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_configs", indexes = {
                @Index(name = "idx_alert_config_monitor", columnList = "monitor_id"),
                @Index(name = "idx_alert_config_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfig {
        @Id
        @GeneratedValue
        private UUID id;

        /**
         * Monitor áp dụng config alert này
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "monitor_id", nullable = false)
        private Monitor monitor;

        /**
         * Kênh gửi cảnh báo: EMAIL, SLACK, ...
         */
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private AlertChannelType type;

        /**
         * Điểm đến nhận cảnh báo
         * Ví dụ:
         * - Email: ops@company.com
         * - Slack: webhook url
         */
        @Column(nullable = false, columnDefinition = "TEXT")
        private String destination;

        @Builder.Default
        @Column(name = "is_enabled", nullable = false)
        private Boolean isEnabled = true;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
}