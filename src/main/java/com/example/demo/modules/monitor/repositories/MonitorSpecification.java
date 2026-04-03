package com.example.demo.modules.monitor.repositories;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;

public class MonitorSpecification {

    public static Specification<Monitor> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Monitor> hasStatus(String statusStr) {
        return (root, query, cb) -> {
            if (statusStr == null || statusStr.isBlank()) {
                return null;
            }
            try {
                MonitorStatus status = MonitorStatus.valueOf(statusStr.toUpperCase());
                return cb.equal(root.get("lastStatus"), status);
            } catch (IllegalArgumentException e) {
                return null;
            }
        };
    }

    public static Specification<Monitor> hasActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return null;
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Monitor> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }
}
