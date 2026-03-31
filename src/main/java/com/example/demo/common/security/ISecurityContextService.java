package com.example.demo.common.security;

import com.example.demo.modules.user.entities.User;
import java.util.Optional;
import java.util.UUID;

public interface ISecurityContextService {
    /**
     * Lấy toàn bộ đối tượng User đang đăng nhập.
     */
    Optional<User> getCurrentUser();

    /**
     * Lấy ID của User đang đăng nhập.
     */
    Optional<UUID> getCurrentUserId();

    /**
     * Kiểm tra xem user có đang đăng nhập hay không.
     */
    boolean isAuthenticated();
}
