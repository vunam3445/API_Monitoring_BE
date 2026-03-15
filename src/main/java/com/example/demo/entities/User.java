package com.example.demo.entities;

import com.example.demo.enums.UserRole;
import com.example.demo.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails {
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


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Chuyển Enum UserRole thành SimpleGrantedAuthority mà Spring Security hiểu được
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // 2. Trả về mật khẩu dùng để xác thực
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    // 3. Trả về định danh (Email)
    @Override
    public String getUsername() {
        return this.email;
    }

    // 4. Các thiết lập trạng thái tài khoản (Tạm thời để true hết)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Bạn có thể logic hóa chỗ này: return this.status != UserStatus.SUSPENDED;
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}