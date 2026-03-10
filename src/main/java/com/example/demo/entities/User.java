package com.example.demo.entities;

import com.example.demo.enums.UserRole;
import com.example.demo.enums.UserStatus;
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

    private String passwordHash;
    private String fullName;
    private String company;

    private String provider = "local";
    private String providerId;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE; // ACTIVE, SUSPENDED

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER; // USER, ADMIN

    private String planType = "FREE";
    private String avatarUrl;

    @Column(unique = true)
    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}