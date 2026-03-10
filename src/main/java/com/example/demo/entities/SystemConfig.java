package com.example.demo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "system_configs")
@Data
public class SystemConfig {
    @Id
    private String configKey; // vd: "SUPPORT_EMAIL", "MAINTENANCE_MODE"
    private String configValue;
    private String description;
}