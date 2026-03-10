package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "alert_configs")
@Data
public class AlertConfig {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    private String type; // EMAIL, TELEGRAM, SLACK
    private String destination;
    private Boolean isEnabled = true;
}