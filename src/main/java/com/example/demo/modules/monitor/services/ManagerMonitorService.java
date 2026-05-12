package com.example.demo.modules.monitor.services;

import com.example.demo.common.exceptions.MonitorNotFoundException;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.MonitorFilterCriteria;
import com.example.demo.modules.monitor.dto.MonitorStatisticsDTO;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.mappers.MonitorMapper;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.monitor.repositories.MonitorSpecification;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import com.example.demo.modules.user.repositories.UserRepository;
import com.example.demo.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.common.base.RestPageImpl;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerMonitorService implements IManagerMonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorMapper mapper;
    private final UptimeLogsRepository uptimeLogsRepository;
    private final UserRepository userRepository;
    private final ICacheService cacheService;

    @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.listener.simple.concurrency:20}")
    private int workerConcurrency;

    @Override
    @Cacheable(value = "api-monitoring:api:list", key = "'manager:list:user_' + (#criteria.userId ?: 'all') + " +
            "':status_' + (#criteria.lastStatus ?: 'all') + " +
            "':active_' + (#criteria.isActive ?: 'all') + " +
            "':search_' + (#criteria.search ?: 'none') + " +
            "':page_' + #pageable.pageNumber + " +
            "':size_' + #pageable.pageSize + " +
            "':sort_' + #pageable.sort.toString().replaceAll(':', '_')", unless = "#result == null")
    public Page<ApiResponse> getAllMonitors(MonitorFilterCriteria criteria, Pageable pageable) {

        Specification<Monitor> spec = (root, query, cb) -> cb.conjunction();

        if (criteria.getUserId() != null) {
            spec = spec.and(MonitorSpecification.hasUserId(criteria.getUserId()));
        }

        if (criteria.getLastStatus() != null && !criteria.getLastStatus().isBlank()) {
            spec = spec.and(MonitorSpecification.hasStatus(criteria.getLastStatus()));
        }

        if (criteria.getIsActive() != null) {
            spec = spec.and(MonitorSpecification.hasActive(criteria.getIsActive()));
        }

        if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
            spec = spec.and(MonitorSpecification.hasNameLike(criteria.getSearch()));
        }

        Page<Monitor> entityPage = monitorRepository.findAll(spec, pageable);

        // --- Tối ưu Uptime % (Phương án A - Bulk Query) ---
        List<UUID> monitorIds = entityPage.getContent().stream()
                .map(Monitor::getId)
                .collect(Collectors.toList());

        Map<UUID, Double> uptimeMap = new HashMap<>();
        if (!monitorIds.isEmpty()) {
            LocalDateTime since = LocalDateTime.now().minusDays(1); // SLA 7 ngày
            List<Object[]> stats = uptimeLogsRepository.getBulkUptimeStats(monitorIds, since);

            for (Object[] row : stats) {
                UUID mid = (UUID) row[0];
                long total = ((Number) row[1]).longValue();
                long up = ((Number) row[2]).longValue();
                double percent = (total > 0) ? (double) up * 100 / total : 100.0;
                uptimeMap.put(mid, Math.round(percent * 100.0) / 100.0); // Làm tròn 2 chữ số
            }
        }

        // --- Tối ưu Owner Name (Bulk Query) ---
        List<UUID> userIds = entityPage.getContent().stream()
                .map(Monitor::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> ownerMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userRepository.findAllById(userIds);
            for (User u : users) {
                ownerMap.put(u.getId(), u.getFullName());
            }
        }

        List<ApiResponse> dtos = entityPage.getContent().stream().map(m -> {
            ApiResponse dto = mapper.toResponse(m);
            dto.setUptimePercentage(uptimeMap.getOrDefault(m.getId(), 100.0));
            dto.setOwnerName(ownerMap.getOrDefault(m.getUserId(), "N/A"));
            return dto;
        }).collect(Collectors.toList());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0);
    }

    public MonitorStatisticsDTO getMonitorStatistics() {
        Object[] result = monitorRepository.countGlobalMonitorStats();

        // Kiểm tra nếu kết quả trả về bị lồng trong một mảng khác (thường gặp trong
        // Hibernate aggregate query)
        Object[] row = (result != null && result.length > 0 && result[0] instanceof Object[])
                ? (Object[]) result[0]
                : result;

        MonitorStatisticsDTO dto = new MonitorStatisticsDTO();
        if (row != null && row.length >= 3) {
            dto.setTotalMonitors(row[0] != null ? ((Number) row[0]).longValue() : 0);
            dto.setActiveMonitors(row[1] != null ? ((Number) row[1]).longValue() : 0);
            dto.setDownMonitors(row[2] != null ? ((Number) row[2]).longValue() : 0);
            
            // Tính toán tài nguyên thực tế
            // Lấy thời gian phản hồi trung bình (nếu null thì giả định 1000ms)
            double avgLatencyMs = (row.length > 3 && row[3] != null) ? ((Number) row[3]).doubleValue() : 1000.0;
            if (avgLatencyMs <= 0) avgLatencyMs = 1000.0;
            
            // Tính toán sức chứa tối đa mỗi phút
            // Công thức: Số Workers * (60000ms / Thời gian phản hồi trung bình)
            // Thêm 100ms overhead cho Database/Network
            double maxCapacityPerMinute = workerConcurrency * (60000.0 / (avgLatencyMs + 100));
            
            // Tính Platform Capacity: Tỷ lệ giữa số Monitor hiện có và sức chứa tối đa
            double capacity = 0;
            if (maxCapacityPerMinute > 0) {
                capacity = (dto.getTotalMonitors() / maxCapacityPerMinute) * 100;
            }
            capacity = Math.min(Math.max(capacity, 0), 100);
            dto.setPlatformCapacity(capacity);
        }
        return dto;
    }

    public Boolean blockMonitor(UUID monitorId) {
        Monitor m = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException("Không tìm thấy monitor!"));
        m.setIsBlock(!m.getIsBlock());
        monitorRepository.save(m);

        // Clear caches
        cacheService.evictByPrefix("api-monitoring:api:list::");
        cacheService.evictByPrefix("monitoring:overview:" + monitorId);
        cacheService.evictByPrefix("monitoring:chart:" + m.getUserId() + ":" + monitorId);
        cacheService.evictByPrefix("monitoring:key-health:" + m.getUserId());
        cacheService.evictByPrefix("monitoring:summary:" + m.getUserId());

        return m.getIsBlock();
    }
}
