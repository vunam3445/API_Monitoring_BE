package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // USER_LOGIN, MONITOR_CREATE, PLAN_CHANGE
    private String targetType; // USER, MONITOR, SUBSCRIPTION
    private String targetId;
    private String performedBy; // UUID của admin hoặc "SYSTEM"

    @Column(columnDefinition = "TEXT")
    private String details; // Nội dung thay đổi (JSON cũ/mới)
    private LocalDateTime timestamp = LocalDateTime.now();
}