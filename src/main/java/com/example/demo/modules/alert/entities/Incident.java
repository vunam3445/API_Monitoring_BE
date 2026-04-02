package com.example.demo.modules.alert.entities;

import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import com.example.demo.modules.monitor.entities.Monitor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(name = "idx_incident_monitor", columnList = "monitor_id"),
                @Index(name = "idx_incident_status", columnList = "status"),
                @Index(name = "idx_incident_severity", columnList = "severity"),
                @Index(name = "idx_incident_triggered_at", columnList = "triggered_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Monitor phát sinh sự cố
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    /**
     * Loại sự cố: DOWN, TIMEOUT, SLOW_RESPONSE...
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentType type;

    /**
     * Mức độ nghiêm trọng
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    /**
     * ACTIVE / ACKNOWLEDGED / RESOLVED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    /**
     * Tiêu đề ngắn cho UI
     */
    @Column(length = 255)
    private String title;

    /**
     * Nội dung chi tiết alert
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Thời điểm incident bắt đầu
     */
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    /**
     * Thời điểm incident được resolve
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Lần cuối vẫn còn thấy lỗi
     */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /**
     * Số lần lỗi liên tiếp
     */
    @Builder.Default
    @Column(name = "consecutive_fail_count", nullable = false)
    private Integer consecutiveFailCount = 1;

    /**
     * Độ trễ trung bình nếu là slow response
     */
    @Column(name = "avg_latency_ms")
    private Long avgLatencyMs;

    /**
     * HTTP status code gần nhất nếu cần
     */
    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    /**
     * Region gây lỗi nếu có multi-region
     */
    @Column(length = 100)
    private String region;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}