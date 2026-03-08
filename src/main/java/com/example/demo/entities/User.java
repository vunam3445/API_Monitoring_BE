package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String email;
    private String password; // Null nếu chỉ dùng Google
    private String provider = "local"; // local, google
    private String providerId;
    private String planType = "FREE"; // Cập nhật dựa trên Subscription
    private LocalDateTime createdAt = LocalDateTime.now();
}