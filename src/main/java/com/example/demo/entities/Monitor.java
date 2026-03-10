package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitors")
@Data
public class Monitor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    private String name;
    @Column(columnDefinition = "TEXT")
    private String url;
    private String method = "GET";

    @Column(columnDefinition = "jsonb")
    private String headers;

    private Integer checkInterval;
    private Boolean isActive = true;
    private Boolean isMuted = false; // Tạm dừng gửi alert
    private String lastStatus = "UNKNOWN";
    private LocalDateTime lastCheckAt;
}