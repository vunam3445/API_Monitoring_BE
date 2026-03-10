package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Data
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    private String type; // EMAIL, TELEGRAM...
    private String status; // SENT, FAILED
    private String message;
    private LocalDateTime sentAt = LocalDateTime.now();
    private String errorMessage; // Nếu gửi thất bại thì lưu ở đây
}