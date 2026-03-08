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
    private String headers; // Lưu JSON headers
    private Integer checkInterval; // Tính bằng giây
    private Boolean isActive = true;
    private String lastStatus = "UNKNOWN";
}