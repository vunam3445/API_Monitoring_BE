package com.example.demo.common.security;

import com.example.demo.modules.user.entities.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("ss")
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final EntityManager entityManager; // Dùng EntityManager để truy vấn mọi loại Entity

    public boolean isOwner(Object targetId, String entityType) {
        log.info(">>> [SecurityCheck] Checking ownership...");
        log.info(">>> Target ID: {}", targetId);
        log.info(">>> Entity Type: {}", entityType);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            log.warn(">>> [SecurityCheck] Authentication failed or user principal is not valid.");
            return false;
        }

        UUID currentUserId = user.getId();

        try {
            String existQueryString;
            if (entityType.equalsIgnoreCase("UserSetting")) {
                existQueryString = "SELECT COUNT(e) FROM UserSetting e WHERE e.userId = :targetId";
            } else {
                existQueryString = String.format("SELECT COUNT(e) FROM %s e WHERE e.id = :targetId", entityType);
            }

            Long existCount = entityManager.createQuery(existQueryString, Long.class)
                    .setParameter("targetId", targetId)
                    .getSingleResult();

            if (existCount == 0) {
                if (entityType.equalsIgnoreCase("Monitor")) {
                    log.error("Không tìm thấy Monitor: {}" + targetId);
                    throw new com.example.demo.common.exceptions.MonitorNotFoundException(
                            "Không tìm thấy Monitor hoặc Monitor đã bị xóa trước đó!");

                } else {
                    throw new com.example.demo.common.exceptions.ResourceNotFoundException(
                            "Không tìm thấy " + entityType + " với ID: " + targetId);
                }
            }

            String queryString;
            if (entityType.equalsIgnoreCase("UserSetting")) {
                // UserSetting mapping qua thuộc tính 'user' (@OneToOne / @ManyToOne)
                queryString = "SELECT COUNT(e) FROM UserSetting e WHERE e.userId = :targetId AND e.user.id = :userId";
            } else {
                // Monitor và nhiều Entity khác mapping trực tiếp qua UUID userId
                queryString = String.format(
                        "SELECT COUNT(e) FROM %s e WHERE e.id = :targetId AND e.userId = :userId",
                        entityType);
            }

            Long count = entityManager.createQuery(queryString, Long.class)
                    .setParameter("targetId", targetId)
                    .setParameter("userId", currentUserId)
                    .getSingleResult();

            boolean result = count > 0;
            log.info(">>> [SecurityCheck] Result: {}", result);
            return result;
        } catch (com.example.demo.common.exceptions.MonitorNotFoundException
                | com.example.demo.common.exceptions.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error(">>> [SecurityCheck] Error during ownership check for {}: {}", entityType, e.getMessage());
            return false;
        }
    }
}
