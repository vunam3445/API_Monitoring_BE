package com.example.demo.services;

import com.example.demo.entities.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("ss")
@RequiredArgsConstructor
public class SecurityService {

    private final EntityManager entityManager; // Dùng EntityManager để truy vấn mọi loại Entity

    public boolean isOwner(Object targetId, String entityType) {
        // 1. Lấy User hiện tại (giống như cũ)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return false;

        UUID currentUserId = user.getId();

        // 2. Logic kiểm tra đặc thù
        try {
            // Ví dụ: Nếu entityType là 'SubscriptionPlan', check xem user có sở hữu gói này không
            // (Thường Plan là do Admin tạo, nhưng ví dụ với MonitorDevice thì sẽ rõ hơn)
            String queryString = String.format(
                    "SELECT COUNT(e) FROM %s e WHERE e.id = :targetId AND e.user.id = :userId",
                    entityType
            );

            Long count = entityManager.createQuery(queryString, Long.class)
                    .setParameter("targetId", targetId)
                    .setParameter("userId", currentUserId)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}