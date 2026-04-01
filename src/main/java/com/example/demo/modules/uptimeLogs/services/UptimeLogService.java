package com.example.demo.modules.uptimeLogs.services;

import com.example.demo.common.base.RestPageImpl;
import com.example.demo.modules.uptimeLogs.dto.UptimeLogResponse;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import com.example.demo.modules.uptimeLogs.mappers.UptimeLogMapper;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý logic nghiệp vụ cho UptimeLogs.
 *
 * Chỉ cung cấp các phương thức đọc (read-only).
 * Worker ghi dữ liệu trực tiếp qua UptimeLogsRepository, không qua service này.
 * → Tuân thủ Single Responsibility: service này chỉ phục vụ API đọc cho frontend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UptimeLogService implements IUptimeLogService {

    private final UptimeLogsRepository uptimeLogsRepository;
    private final UptimeLogMapper uptimeLogMapper;

    private static final String CACHE_UPTIME_LOGS = "api-monitoring:uptime-logs";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_UPTIME_LOGS,
            key = "'monitor_' + #monitorId.toString() + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize",
            unless = "#result == null")
    public Page<UptimeLogResponse> findByMonitorId(UUID monitorId, Pageable pageable) {
        Page<UptimeLogs> entityPage = uptimeLogsRepository
                .findByMonitorIdOrderByCheckedAtDesc(monitorId, pageable);

        List<UptimeLogResponse> dtos = uptimeLogMapper.toResponseList(entityPage.getContent());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UptimeLogResponse> findByMonitorIdAndDateRange(
            UUID monitorId, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Page<UptimeLogs> entityPage = uptimeLogsRepository
                .findByMonitorIdAndCheckedAtBetweenOrderByCheckedAtDesc(monitorId, from, to, pageable);

        List<UptimeLogResponse> dtos = uptimeLogMapper.toResponseList(entityPage.getContent());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_UPTIME_LOGS,
            key = "'uptime_pct_' + #monitorId.toString()",
            unless = "#result == null")
    public Double calculateUptimePercentage(UUID monitorId) {
        long totalChecks = uptimeLogsRepository.countByMonitorId(monitorId);
        if (totalChecks == 0) {
            return null; // Chưa có dữ liệu
        }

        long upChecks = uptimeLogsRepository.countByMonitorIdAndIsUp(monitorId, true);
        return Math.round((double) upChecks / totalChecks * 10000.0) / 100.0; // Làm tròn 2 chữ số
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_UPTIME_LOGS,
            key = "'user_' + #userId.toString() + '_search_' + (#search ?: 'none') + '_code_' + (#statusCode ?: 'all') + '_method_' + (#method ?: 'all') + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString().replaceAll(':', '_')",
            unless = "#result == null")
    public Page<UptimeLogResponse> findLogsByUser(UUID userId, String search, Integer statusCode, String method, Pageable pageable) {
        Specification<UptimeLogs> spec = Specification.where(UptimeLogsSpecification.hasUserId(userId))
                .and(UptimeLogsSpecification.hasMonitorNameLike(search))
                .and(UptimeLogsSpecification.hasStatusCode(statusCode))
                .and(UptimeLogsSpecification.hasMonitorMethod(method));

        Page<UptimeLogs> entityPage = uptimeLogsRepository.findAll(spec, pageable);
        List<UptimeLogResponse> dtos = uptimeLogMapper.toResponseList(entityPage.getContent());

        return new RestPageImpl<>(
                dtos,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                null, false, 0, null, false, 0
        );
    }
}

