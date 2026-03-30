package com.example.demo.modules.uptimeLogs.repositories;

import com.example.demo.modules.monitor.entities.Monitor;
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
}
