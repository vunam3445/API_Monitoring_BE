package com.example.demo.modules.system.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ log lỗi của chính ứng dụng Backend (Application/System Logs).
 * KHÔNG nhầm lẫn với UptimeLogs - đó là log trạng thái ping API của người dùng.
 */
@Entity
@Table(name = "application_logs", indexes = {
        @Index(name = "idx_app_log_timestamp", columnList = "timestamp"),
        @Index(name = "idx_app_log_level", columnList = "level"),
        @Index(name = "idx_app_log_component", columnList = "component")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "level", nullable = false, length = 10)
    private String level;

    @Column(name = "component", nullable = false, length = 255)
    private String component;

    @Column(name = "thread_id", length = 100)
    private String threadId;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
}
