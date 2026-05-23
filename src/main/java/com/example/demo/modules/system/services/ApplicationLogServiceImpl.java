package com.example.demo.modules.system.services;

import com.example.demo.modules.system.entities.ApplicationLog;
import com.example.demo.modules.system.repositories.ApplicationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hiện thực hóa IApplicationLogService:
 * - Tìm kiếm log đa tiêu chí (level, keyword, timeRange) với phân trang.
 * - Thống kê log trong ngày hôm nay.
 * - Dọn dẹp log thủ công + Cron Job tự động lúc 02:00 AM hàng ngày.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationLogServiceImpl implements IApplicationLogService {

    private final ApplicationLogRepository applicationLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationLog> getLogs(int page, int size, String level, String keyword, String timeRange) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Specification<ApplicationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo Level (bỏ qua nếu là "ALL")
            if (level != null && !level.isBlank() && !"ALL".equalsIgnoreCase(level)) {
                predicates.add(cb.equal(root.get("level"), level.toUpperCase()));
            }

            // 2. Tìm kiếm từ khóa không phân biệt hoa/thường trên message, component, threadId
            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                Predicate msgPredicate = cb.like(cb.lower(root.get("message")), likeKeyword);
                Predicate compPredicate = cb.like(cb.lower(root.get("component")), likeKeyword);
                Predicate threadPredicate = cb.like(cb.lower(root.get("threadId")), likeKeyword);
                predicates.add(cb.or(msgPredicate, compPredicate, threadPredicate));
            }

            // 3. Lọc theo khoảng thời gian
            if (timeRange != null && !timeRange.isBlank() && !"ALL".equalsIgnoreCase(timeRange)) {
                LocalDateTime threshold = switch (timeRange.toLowerCase()) {
                    case "5m"  -> LocalDateTime.now().minusMinutes(5);
                    case "1h"  -> LocalDateTime.now().minusHours(1);
                    case "24h" -> LocalDateTime.now().minusHours(24);
                    default    -> null;
                };
                if (threshold != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), threshold));
                }
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };

        return applicationLogRepository.findAll(spec, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        // Chỉ lấy các log từ đầu ngày hôm nay
        Specification<ApplicationLog> todaySpec = (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("timestamp"), startOfToday);

        List<ApplicationLog> todayLogs = applicationLogRepository.findAll(todaySpec);

        long warnings = todayLogs.stream().filter(l -> "WARN".equalsIgnoreCase(l.getLevel())).count();
        long errors   = todayLogs.stream().filter(l -> "ERROR".equalsIgnoreCase(l.getLevel())).count();
        long fatals   = todayLogs.stream().filter(l -> "FATAL".equalsIgnoreCase(l.getLevel())).count();
        // INFO luôn là 0 theo Phương án A: chỉ lưu WARN/ERROR/FATAL
        long infos    = 0;
        long total    = warnings + errors + fatals;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("infos", infos);
        stats.put("warnings", warnings);
        stats.put("errors", errors);
        stats.put("fatals", fatals);
        return stats;
    }

    @Override
    @Transactional
    public Map<String, Object> clearOldLogs(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = applicationLogRepository.deleteLogsOlderThan(threshold);
        log.info("[ApplicationLogService] Cleared {} system logs older than {} days.", deletedCount, retentionDays);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Successfully cleared " + deletedCount
                + " old log records older than " + retentionDays + " days.");
        result.put("deletedCount", deletedCount);
        result.put("retentionDays", retentionDays);
        return result;
    }

    /**
     * Cron Job tự động dọn dẹp log cũ hơn 30 ngày vào lúc 02:00 AM hàng ngày.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoClearOldLogs() {
        log.info("[ApplicationLogService] Starting automatic system logs cleanup (retention: 30 days)...");
        clearOldLogs(30);
    }
}
