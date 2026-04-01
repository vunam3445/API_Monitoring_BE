package com.example.demo.modules.uptimeLogs.repositories;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;

public class UptimeLogsSpecification {

    public static Specification<UptimeLogs> hasUserId(UUID userId) {
        return (root, query, cb) -> {
            Join<UptimeLogs, Monitor> monitorJoin = root.join("monitor");
            return cb.equal(monitorJoin.get("userId"), userId);
        };
    }

    public static Specification<UptimeLogs> hasMonitorNameLike(String searchName) {
        return (root, query, cb) -> {
            if (searchName == null || searchName.isBlank()) {
                return null;
            }
            Join<UptimeLogs, Monitor> monitorJoin = root.join("monitor");
            return cb.like(cb.lower(monitorJoin.get("name")), "%" + searchName.toLowerCase() + "%");
        };
    }

    public static Specification<UptimeLogs> hasMonitorMethod(String method) {
        return (root, query, cb) -> {
            if (method == null || method.isBlank()) {
                return null;
            }
            Join<UptimeLogs, Monitor> monitorJoin = root.join("monitor");
            return cb.equal(cb.upper(monitorJoin.get("method")), method.toUpperCase());
        };
    }

    public static Specification<UptimeLogs> hasStatusCode(Integer statusCode) {
        return (root, query, cb) -> {
            if (statusCode == null) {
                return null;
            }
            return cb.equal(root.get("statusCode"), statusCode);
        };
    }
 
    public static Specification<UptimeLogs> hasMonitorId(UUID monitorId) {
        return (root, query, cb) -> {
            if (monitorId == null) return null;
            return cb.equal(root.get("monitorId"), monitorId);
        };
    }
 
    public static Specification<UptimeLogs> hasEventType(MonitorEventType eventType) {
        return (root, query, cb) -> {
            if (eventType == null) return null;
            return cb.equal(root.get("eventType"), eventType);
        };
    }
 
    public static Specification<UptimeLogs> hasStatus(MonitorStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            switch (status) {
                case HEALTHY -> {
                    return cb.and(
                        cb.equal(root.get("isUp"), true),
                        cb.equal(root.get("assertionStatus"), "PASSED")
                    );
                }
                case WARNING -> {
                    return cb.and(
                        cb.equal(root.get("isUp"), true),
                        cb.equal(root.get("assertionStatus"), "WARNING")
                    );
                }
                case DOWN -> {
                    return cb.equal(root.get("isUp"), false);
                }
                default -> { return null; }
            }
        };
    }
}

