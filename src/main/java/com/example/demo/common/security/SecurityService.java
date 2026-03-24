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
            // JPQL: Truy vấn động dựa trên entityType. Giả định Entity có field 'user'
            // Đối với UserSetting, field ID là 'userId', nhưng ta sẽ dùng mapping linh hoạt
            String queryString = String.format(
                    "SELECT COUNT(e) FROM %s e WHERE (e.id = :targetId OR (KEY(e) IS NULL AND e.userId = :targetId)) AND e.user.id = :userId",
                    entityType
            );
            
            // Một số entity có thể dùng field khác hoặc tên ID khác, tạm thời dùng standard:
            if (entityType.equals("UserSetting")) {
                queryString = "SELECT COUNT(e) FROM UserSetting e WHERE e.userId = :targetId AND e.user.id = :userId";
            }

            Long count = entityManager.createQuery(queryString, Long.class)
                    .setParameter("targetId", targetId)
                    .setParameter("userId", currentUserId)
                    .getSingleResult();

            boolean result = count > 0;
            log.info(">>> [SecurityCheck] Result: {}", result);
            return result;
        } catch (Exception e) {
            log.error(">>> [SecurityCheck] Error during ownership check for {}: {}", entityType, e.getMessage());
            return false;
        }
    }
}
