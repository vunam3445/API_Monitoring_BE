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
        List<Object[]> results = monitorRepository.countGlobalMonitorStats();
        Object[] row = (results != null && !results.isEmpty()) ? results.get(0) : null;

        MonitorStatisticsDTO dto = new MonitorStatisticsDTO();
        if (row != null && row.length >= 3) {
            dto.setTotalMonitors(row[0] != null ? ((Number) row[0]).longValue() : 0);
            dto.setActiveMonitors(row[1] != null ? ((Number) row[1]).longValue() : 0);
            dto.setDownMonitors(row[2] != null ? ((Number) row[2]).longValue() : 0);
            
            // Tính toán Platform Capacity theo Worker Occupancy
            // row[4] là tổng occupancy ratio của các monitor đang hoạt động
            double totalOccupancy = (row.length > 4 && row[4] != null) ? ((Number) row[4]).doubleValue() : 0.0;
            
            // Platform Capacity = (Tổng Occupancy / Số lượng Worker) * 100
            double capacityRatio = 0;
            if (workerConcurrency > 0) {
                capacityRatio = (totalOccupancy / workerConcurrency) * 100;
            }
            
            // Giới hạn trong khoảng [0, 100] và gán vào DTO
            capacityRatio = Math.min(Math.max(capacityRatio, 0), 100);
            dto.setPlatformCapacity(capacityRatio);
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
