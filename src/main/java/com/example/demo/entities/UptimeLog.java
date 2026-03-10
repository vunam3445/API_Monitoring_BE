package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "uptime_logs", indexes = {
        @Index(name = "idx_monitor_time", columnList = "monitor_id, timestamp")
})
@Data
public class UptimeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    private Integer statusCode;
    private Integer responseTimeMs;
    private Boolean isUp;
    private LocalDateTime timestamp = LocalDateTime.now();
}